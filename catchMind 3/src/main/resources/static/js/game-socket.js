// ===============================
// 0. 대기방: 플레이어 목록 갱신 함수
//  - 대기방에서 현재 접속자 이름 목록을 갱신하고
//  - 인원 수/정원/시작 버튼 상태도 함께 업데이트한다.
// ===============================
function updatePlayerList(players) {
  const listEl = document.getElementById("waiting-player-list");
  if (!listEl) return;

  // (1) 목록 비우고 다시 채우기
  listEl.innerHTML = "";
  players.forEach(function (name) {
    const li = document.createElement("li");
    li.className = "list-group-item py-1";
    li.textContent = name;
    listEl.appendChild(li);
  });

  // (2) 현재 인원 숫자 바꾸기
  const countEl = document.getElementById("current-count");
  if (countEl) {
    countEl.textContent = players.length;
  }

  // (3) 정원 읽어오기
  const capacityEl = document.getElementById("room-capacity");
  const capacity = capacityEl ? parseInt(capacityEl.textContent, 10) : null;

  const startBtn = document.getElementById("btn-start-game");
  const waitMsg = document.getElementById("msg-wait");
  const fullMsg = document.getElementById("msg-full");

  // (4) 정원에 따라 버튼 / 안내 문구 처리
  if (capacity != null) {
    const isFull = players.length >= capacity;

    if (startBtn) {
      startBtn.disabled = !isFull;
    }

    if (waitMsg) {
      waitMsg.style.display = isFull ? "none" : "";
    }
    if (fullMsg) {
      fullMsg.style.display = isFull ? "" : "none";
    }
  }
}

(function () {
  "use strict";

  // ===============================
  // 1. 기본 설정 및 공통 변수
  //  - 메타 태그에서 방 ID, 내 닉네임, 역할을 읽어온다.
  //  - WebSocket 연결을 생성한다.
  // ===============================

  // 내 역할 (DRAWER / GUESSER / "")
  const roleMeta = document.querySelector('meta[name="my-role"]');
  const myRole = roleMeta ? roleMeta.content : null;

  // 방 / 닉네임 정보
  const roomMeta = document.querySelector('meta[name="room-id"]');
  const nameMeta = document.querySelector('meta[name="my-name"]');
  const roomId = roomMeta ? roomMeta.content : null;
  const myName = nameMeta ? nameMeta.content : null;

  // WebSocket URL 생성
  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";

  const socket = new WebSocket(socketUrl);

  // 디버깅용 로그 함수
  function log() {
    const args = Array.prototype.slice.call(arguments);
    args.unshift("[WS]");
    console.log.apply(console, args);
  }

  // 서버로 메시지 전송
  function send(message) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    } else {
      log("socket not open, cannot send:", message);
    }
  }

  // WebSocket open 이벤트 핸들러
  function onSocketOpen() {
    log("connected");

    // 연결되면 JOIN 메시지 한 번 전송
    if (roomId && myName) {
      send({
        type: "JOIN",
        roomId: roomId,
        nickname: myName,
        role: myRole // DRAWER / GUESSER / ""
      });
    } else {
      log("JOIN not sent - roomId or myName missing", {
        roomId: roomId,
        myName: myName,
        myRole: myRole
      });
    }
  }

  // WebSocket close 이벤트 핸들러
  function onSocketClose() {
    log("closed");
  }

  // WebSocket error 이벤트 핸들러
  function onSocketError(e) {
    log("error", e);
  }

  socket.addEventListener("open", onSocketOpen);
  socket.addEventListener("close", onSocketClose);
  socket.addEventListener("error", onSocketError);

  // ===============================
  // 2. 캔버스(그림판) 관련 설정
  //  - Drawer는 마우스로 그림을 그리고, 그 정보를 서버로 보낸다.
  //  - Drawer/Guesser 모두 DRAW 메시지를 받아서 화면에 그림을 그린다.
  // ===============================

  const canvas = document.getElementById("drawing-canvas");
  const ctx = canvas ? canvas.getContext("2d") : null;

  // 캔버스 크기를 부모 요소 비율에 맞게 조정
  function resizeCanvas() {
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width;
    canvas.height = rect.height;
  }

  if (canvas && ctx) {
    resizeCanvas();
    window.addEventListener("resize", resizeCanvas);
  }

  // 캔버스를 모두 지우는 함수 (필요 시 사용)
  function clearCanvas() {
    if (!ctx || !canvas) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }

  // 한 번의 선(또는 점)을 그리는 함수
  function drawLine(x1, y1, x2, y2, color, width) {
    if (!ctx) return;

    const lineColor = color || "#000000";
    const lineWidth = width || 4;

    ctx.strokeStyle = lineColor;
    ctx.lineWidth = lineWidth;
    ctx.lineCap = "round";

    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();
  }

  // 마우스 드로잉 관련 변수
  let drawing = false;
  let lastX = 0;
  let lastY = 0;

  // 캔버스 내 좌표 변환
  function getCanvasPos(e) {
    const rect = canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top
    };
  }

  // 서버로 DRAW 메시지 전송
  function sendDrawPoint(x, y, dragging) {
    send({
      type: "DRAW",
      roomId: roomId,
      x: x,
      y: y,
      color: "#000000",
      thickness: 4,
      dragging: dragging // true면 이전 점과 이어지는 선
    });
  }

  // Drawer일 때만 로컬에서 그림 + 서버 전송
  function onCanvasMouseDown(e) {
    if (!canvas) return;

    drawing = true;
    const pos = getCanvasPos(e);
    lastX = pos.x;
    lastY = pos.y;

    // 새 스트로크 시작점: dragging = false
    sendDrawPoint(pos.x, pos.y, false);
  }

  function onCanvasMouseMove(e) {
    if (!drawing || !canvas) return;

    const pos = getCanvasPos(e);
    drawLine(lastX, lastY, pos.x, pos.y, "#000000", 4);
    sendDrawPoint(pos.x, pos.y, true);

    lastX = pos.x;
    lastY = pos.y;
  }

  function onMouseUp() {
    drawing = false;
  }

  function onCanvasMouseLeave() {
    drawing = false;
  }

  if (canvas && ctx && myRole === "DRAWER") {
    canvas.addEventListener("mousedown", onCanvasMouseDown);
    canvas.addEventListener("mousemove", onCanvasMouseMove);
    window.addEventListener("mouseup", onMouseUp);
    canvas.addEventListener("mouseleave", onCanvasMouseLeave);
  }

  // DRAW 메시지 수신 처리용 변수
  let hasPrevPoint = false;
  let prevX = 0;
  let prevY = 0;

  // 서버에서 받은 DRAW 메시지 처리
  function handleDraw(msg) {
    if (!ctx || !canvas) return;

    const x = msg.x;
    const y = msg.y;
    const color = msg.color || "#000000";
    const thickness = msg.thickness || 4;
    const dragging = !!msg.dragging;

    if (dragging && hasPrevPoint) {
      // 이전 점과 현재 점을 선으로 연결
      drawLine(prevX, prevY, x, y, color, thickness);
    } else {
      // 새로운 점(원의 형태)으로 찍기
      ctx.beginPath();
      ctx.arc(x, y, thickness / 2, 0, Math.PI * 2);
      ctx.fillStyle = color;
      ctx.fill();
      ctx.closePath();
    }

    prevX = x;
    prevY = y;
    hasPrevPoint = true;
  }

  // ===============================
  // 3. GUESSER: 정답 입력 처리
  //  - 정답 입력 폼 submit을 가로채서 fetch로 서버에 전송한다.
  // ===============================

  const guessForm = document.getElementById("guess-form");
  const answerInput = document.getElementById("answer-input");

  function onGuessFormSubmit(e) {
    e.preventDefault(); // 페이지 이동 막기

    if (!guessForm || !answerInput) return;

    const value = answerInput.value.trim();
    if (!value) return;

    const formData = new FormData(guessForm);

    fetch(guessForm.action, {
      method: "POST",
      body: formData
    }).catch(function (err) {
      console.error("submit error", err);
    });

    answerInput.value = "";
    answerInput.focus();
  }

  if (guessForm && answerInput) {
    guessForm.addEventListener("submit", onGuessFormSubmit);
  }

  // ===============================
  // 4. 라운드/점수/타이머 & 결과 UI
  //  - 서버에서 온 결과 메시지에 따라 점수/라운드/기회 수 UI를 갱신한다.
  //  - 타이머는 클라이언트에서 90초를 카운트하고, 끝나면 서버에 TIME_OUT을 알린다.
  // ===============================

  // 타이머 관련 변수
  let timerInterval;
  const ROUND_DURATION = 90; // 1분 30초

  // 초 단위를 "분:초" 형태 문자열로 변환
  function formatTime(seconds) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    const padded =
      remainingSeconds < 10 ? "0" + remainingSeconds : String(remainingSeconds);
    return minutes + ":" + padded;
  }

  // 타이머 숫자 표시 갱신
  function updateTimerDisplay(seconds) {
    const timerEl = document.getElementById("timer-display");
    if (timerEl) {
      timerEl.textContent = formatTime(seconds);
    }
  }

  // 타이머 시작
  function startTimer() {
    // 기존 타이머 정리
    if (timerInterval) {
      clearInterval(timerInterval);
    }

    let timeLeft = ROUND_DURATION;
    updateTimerDisplay(timeLeft);

    timerInterval = setInterval(function () {
      timeLeft--;
      updateTimerDisplay(timeLeft);

      if (timeLeft <= 0) {
        clearInterval(timerInterval);
        timerInterval = null;
        // 타이머 종료 시 서버로 TIME_OUT 전송
        sendTimeOut();
      }
    }, 1000);
  }

  // 타이머 중지
  function stopTimer() {
    if (timerInterval) {
      clearInterval(timerInterval);
      timerInterval = null;
    }
  }

  // TIME_OUT 서버 전송 (라운드 시간 종료 알림)
  function sendTimeOut() {
    if (!roomId || !myName) {
      log("TIME_OUT not sent - roomId or myName missing", {
        roomId: roomId,
        myName: myName
      });
      return;
    }

    fetch("/game/timeout", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        roomId: roomId,
        userId: myName // userId로 사용
      })
    })
      .then(function (response) {
        if (!response.ok) {
          console.error("TIME_OUT request failed");
        }
      })
      .catch(function (err) {
        console.error("TIME_OUT fetch error", err);
      });
  }

  // 남은 기회(클로버 아이콘) 표시
  function renderAttempts(triesLeft) {
    const container = document.getElementById("attempt-icons");
    if (!container) return;

    container.innerHTML = "";

    for (let i = 0; i < triesLeft; i++) {
      const img = document.createElement("img");
      img.src = "/image/clover_icon.png";
      img.alt = "Attempt";
      img.style.height = "18px";
      img.style.margin = "0 2px";
      container.appendChild(img);
    }
  }

  // 서버에서 받은 GUESS_RESULT 메시지 처리
  function handleGuessResult(msg) {
    const box = document.getElementById("guess-result");
    if (box) {
      if (msg.correct) {
        box.textContent =
          "정답입니다! (이번 라운드 점수: " +
          msg.roundScore +
          ", 총점: " +
          msg.totalScore +
          ")";
        box.className = "mt-2 fw-semibold text-success";
      } else {
        if (typeof msg.triesLeft === "number") {
          box.textContent =
            "틀렸습니다. 다시 시도해보세요! ( 남은 기회: " +
            msg.triesLeft +
            "번 )";
        } else {
          box.textContent = "틀렸습니다. 다시 시도해보세요!";
        }
        box.className = "mt-2 fw-semibold text-danger";
      }
    }

    if (typeof msg.triesLeft === "number") {
      renderAttempts(msg.triesLeft);
    }

    if (typeof msg.totalScore === "number") {
      const scoreSpan = document.getElementById("score");
      if (scoreSpan) {
        scoreSpan.textContent = msg.totalScore;
      }
    }

    if (typeof msg.round === "number") {
      const roundSpan = document.getElementById("round-info");
      if (roundSpan) {
        roundSpan.textContent = msg.round + " / 6";
      }
    }
  }

  // 라운드 종료 처리 (ROUND_END 메시지)
  function handleRoundEnd(msg) {
    stopTimer();
    // midResult 페이지로 이동
    window.location.href = "/game/answer";
  }

  // 다음 라운드로 넘어가기 (ROUND_NEXT 메시지)
  function handleRoundNext(msg) {
    if (window.location.pathname === "/game/answer") {
      const form = document.createElement("form");
      form.method = "post";
      form.action = "/game/next-round";
      document.body.appendChild(form);
      form.submit();
    }
  }

  // ===============================
  // 5. 게임 시작 관련 처리
  //  - HOST(또는 방장)가 GAME_START를 보내면 모든 인원을 /game/begin 으로 이동시킨다.
  // ===============================

  // GAME_START 수신: 모든 플레이어를 동시에 /game/begin 으로 이동
  function handleGameStart(msg) {
    if (!roomId || !myName) {
      return;
    }

    const url =
      "/game/begin?roomId=" +
      encodeURIComponent(roomId) +
      "&userId=" +
      encodeURIComponent(myName);

    window.location.href = url;
  }

  // 대기방에서 "게임 시작" 버튼 클릭 시 GAME_START 전송
  const startBtn = document.getElementById("btn-start-game");

  function onStartButtonClick() {
    // 아직 인원 안 찼으면 무시
    if (startBtn.disabled) {
      return;
    }

    if (!roomId || !myName) {
      log("GAME_START not sent - roomId or myName missing", {
        roomId: roomId,
        myName: myName
      });
      return;
    }

    send({
      type: "GAME_START",
      roomId: roomId,
      nickname: myName
    });
  }

  if (startBtn) {
    startBtn.addEventListener("click", onStartButtonClick);
  }

  // ===============================
  // 6. WebSocket 메시지 수신 분기
  //  - 서버에서 오는 type에 따라 각각 처리 함수를 호출한다.
  // ===============================

  function onSocketMessage(event) {
    let msg;

    try {
      msg = JSON.parse(event.data);
    } catch (e) {
      log("invalid json", event.data);
      return;
    }

    switch (msg.type) {
      case "DRAW":
        handleDraw(msg);
        break;

      case "GUESS_RESULT":
        handleGuessResult(msg);
        break;

      case "ROUND_END":
        handleRoundEnd(msg);
        break;

      case "ROUND_NEXT":
        handleRoundNext(msg);
        break;

      case "GAME_START":
        handleGameStart(msg);
        break;

      case "ROUND_START":
        // 라운드 시작 시 타이머 시작
        log("ROUND_START:", msg);
        startTimer();
        break;

      case "PLAYER_LIST":
        updatePlayerList(msg.players);
        break;

      default:
        log("unknown message type:", msg);
    }
  }

  socket.addEventListener("message", onSocketMessage);

  // ===============================
  // 7. 디버깅용 전역 객체
  //  - 브라우저 콘솔에서 _gameSocket.socket / _gameSocket.send 사용 가능
  // ===============================
  window._gameSocket = {
    socket: socket,
    send: send
  };
})();