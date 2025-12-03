// static/js/game-socket.js

// ===== 대기방 플레이어 목록 갱신 =====
function updatePlayerList(players) {
  const listEl = document.getElementById("waiting-player-list");
  if (!listEl) return;

  // 1) 목록 비우고 다시 채우기
  listEl.innerHTML = "";
  players.forEach((name) => {
    const li = document.createElement("li");
    li.className = "list-group-item py-1";
    li.textContent = name;
    listEl.appendChild(li);
  });

  // 2) 현재 인원 숫자 바꾸기
  const countEl = document.getElementById("current-count");
  if (countEl) {
    countEl.textContent = players.length;
  }

  // 3) 정원 읽어오기
  const capacityEl = document.getElementById("room-capacity");
  const capacity = capacityEl ? parseInt(capacityEl.textContent, 10) : null;

  const startBtn = document.getElementById("btn-start-game");
  const waitMsg = document.getElementById("msg-wait");
  const fullMsg = document.getElementById("msg-full");

  if (capacity != null) {
    const isFull = players.length >= capacity;

    // 4) 버튼 활성/비활성
    if (startBtn) {
      startBtn.disabled = !isFull;
    }

    // 5) 안내 문구 토글
    if (waitMsg) {
      waitMsg.style.display = isFull ? "none" : "";
    }
    if (fullMsg) {
      fullMsg.style.display = isFull ? "" : "none";
    }
  }
}



(function () {
  // ===============================
  // 0. 공통: WebSocket 연결
  // ===============================

  // 내 역할(DRAWER / GUESSER, 혹은 빈 문자열)
  const roleMeta = document.querySelector('meta[name="my-role"]');
  const myRole = roleMeta ? roleMeta.content : null;

  // 방/닉네임 정보 (JOIN, DRAW, GAME_START 때 같이 보냄)
  const roomMeta = document.querySelector('meta[name="room-id"]');
  const nameMeta = document.querySelector('meta[name="my-name"]');
  const roomId = roomMeta ? roomMeta.content : null;
  const myName = nameMeta ? nameMeta.content : null;

  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";

  const socket = new WebSocket(socketUrl);

  function log(...args) {
    console.log("[WS]", ...args);
  }

  function send(message) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    } else {
      log("socket not open, cannot send:", message);
    }
  }

  socket.addEventListener("open", () => {
    log("connected");

    // 연결되면 JOIN 메시지 한 번 전송
    if (roomId && myName) {
      send({
        type: "JOIN",
        roomId: roomId,
        nickname: myName,
        role: myRole, // DRAWER / GUESSER / ""
      });
    } else {
      log("JOIN not sent - roomId or myName missing", {
        roomId,
        myName,
        myRole,
      });
    }
  });

  socket.addEventListener("close", () => log("closed"));
  socket.addEventListener("error", (e) => log("error", e));

  // ===============================
  // 1. 캔버스 기본 세팅 (게임 화면에서만 존재)
  // ===============================
  const canvas = document.getElementById("drawing-canvas");
  const ctx = canvas ? canvas.getContext("2d") : null;

  if (canvas && ctx) {
    function resizeCanvas() {
      const rect = canvas.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;
    }

    resizeCanvas();
    window.addEventListener("resize", resizeCanvas);
  }

  function clearCanvas() {
    if (!ctx || !canvas) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }

  function drawLine(x1, y1, x2, y2, color = "#000000", width = 4) {
    if (!ctx) return;
    ctx.strokeStyle = color;
    ctx.lineWidth = width;
    ctx.lineCap = "round";

    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();
  }

  let drawing = false;
  let lastX = 0;
  let lastY = 0;

  function getCanvasPos(e) {
    const rect = canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    };
  }

  function sendDrawPoint(x, y, dragging) {
    send({
      type: "DRAW",
      roomId: roomId,
      x: x,
      y: y,
      color: "#000000",
      thickness: 4,
      dragging: dragging, // true면 이전 점과 이어지는 선
    });
  }

  // Drawer일 때만 로컬에서 그리기 + 서버로 전송
  if (canvas && ctx && myRole === "DRAWER") {
    canvas.addEventListener("mousedown", (e) => {
      drawing = true;
      const pos = getCanvasPos(e);
      lastX = pos.x;
      lastY = pos.y;

      // 새 스트로크 시작점: dragging = false
      sendDrawPoint(pos.x, pos.y, false);
    });

    canvas.addEventListener("mousemove", (e) => {
      if (!drawing) return;
      const pos = getCanvasPos(e);

      drawLine(lastX, lastY, pos.x, pos.y, "#000000", 4);
      sendDrawPoint(pos.x, pos.y, true);

      lastX = pos.x;
      lastY = pos.y;
    });

    window.addEventListener("mouseup", () => {
      drawing = false;
    });

    canvas.addEventListener("mouseleave", () => {
      drawing = false;
    });
  }

  // ===============================
  // 2. DRAW 메시지 수신 처리 (Drawer & Guesser 둘 다)
  // ===============================
  let hasPrevPoint = false;
  let prevX = 0;
  let prevY = 0;

  function handleDraw(msg) {
    if (!ctx || !canvas) return;

    const x = msg.x;
    const y = msg.y;
    const color = msg.color || "#000000";
    const thickness = msg.thickness || 4;
    const dragging = !!msg.dragging;

    if (dragging && hasPrevPoint) {
      drawLine(prevX, prevY, x, y, color, thickness);
    } else {
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
  // 3. GUESSER: 정답 전송 (폼 submit 가로채기)
  // ===============================
  const guessForm = document.getElementById("guess-form");
  const answerInput = document.getElementById("answer-input");

  if (guessForm && answerInput) {
    guessForm.addEventListener("submit", (e) => {
      e.preventDefault(); // 페이지 이동 막기

      const value = answerInput.value.trim();
      if (!value) return;

      const formData = new FormData(guessForm);

      fetch(guessForm.action, {
        method: "POST",
        body: formData,
      }).catch((err) => {
        console.error("submit error", err);
      });

      answerInput.value = "";
      answerInput.focus();
    });
  }

  // ===============================
  // 4. 라운드/점수/타이머 & 결과 UI
  // ===============================


  // 타이머 변수
  let timerInterval;
  const ROUND_DURATION = 90; // 1분 30초

  function formatTime(seconds) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds < 10 ? '0' : ''}${remainingSeconds}`;
  }

  function updateTimerDisplay(seconds) {
    const timerEl = document.getElementById("timer-display");
    if (timerEl) {
        timerEl.textContent = formatTime(seconds);
    }
  }

  function startTimer() {
    // 기존 타이머 중지
    if (timerInterval) {
        clearInterval(timerInterval);
    }

    let timeLeft = ROUND_DURATION;
    updateTimerDisplay(timeLeft); // 초기 값 표시

    timerInterval = setInterval(() => {
        timeLeft--;
        updateTimerDisplay(timeLeft);

        if (timeLeft <= 0) {
            clearInterval(timerInterval);
            // 타이머 종료 시 서버로 TIME_OUT 메시지 전송
            sendTimeOut();
        }
    }, 1000);
  }

  function stopTimer() {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
  }

  function sendTimeOut() {
    if (!roomId || !myName) {
        log("TIME_OUT not sent - roomId or myName missing", { roomId, myName });
        return;
    }

    // 서버의 GameController /game/timeout POST 엔드포인트 호출
    fetch("/game/timeout", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            roomId: roomId,
            userId: myName // userId로 사용
        })
    }).then(response => {
        if (!response.ok) {
            console.error("TIME_OUT request failed");
        }
    }).catch(err => {
        console.error("TIME_OUT fetch error", err);
    });
  }

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

  function handleGuessResult(msg) {
    const box = document.getElementById("guess-result");
    if (box) {
      if (msg.correct) {
        box.textContent = `정답입니다! (이번 라운드 점수: ${msg.roundScore}, 총점: ${msg.totalScore})`;
        box.className = "mt-2 fw-semibold text-success";
      } else {
        if (typeof msg.triesLeft === "number") {
          box.textContent = `틀렸습니다. 다시 시도해보세요! ( 남은 기회: ${msg.triesLeft}번 )`;
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

  function handleRoundEnd(msg) {
    stopTimer();
    // midResult 페이지로 이동
    window.location.href = "/game/answer";
  }

  function handleRoundNext(msg) {
    if (window.location.pathname === "/game/answer") {
      const form = document.createElement("form");
      form.method = "post";
      form.action = "/game/next-round";
      document.body.appendChild(form);
      form.submit();
    }
  }

  // ===== GAME_START: 모든 플레이어를 동시에 /game/begin 으로 보내기 =====
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

  // ===== 대기방에서 "게임 시작" 버튼 클릭 시 GAME_START 전송 =====
  const startBtn = document.getElementById("btn-start-game");
  if (startBtn) {
    startBtn.addEventListener("click", () => {
      // 아직 인원 안 찼으면 무시
      if (startBtn.disabled) {
        return;
      }

      if (!roomId || !myName) {
        log("GAME_START not sent - roomId or myName missing", {
          roomId,
          myName,
        });
        return;
      }

      send({
        type: "GAME_START",
        roomId: roomId,
        nickname: myName,
      });
    });
  }


  // ===============================
  // 5. WebSocket 메시지 분기 처리
  // ===============================
  socket.addEventListener("message", (event) => {
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
        // 나중에 라운드 시작 UI 갱신용으로 쓸 수 있음
        log("ROUND_START:", msg);
        startTimer();
        break;

      case "PLAYER_LIST":
        updatePlayerList(msg.players);
        break;

      default:
        log("unknown message type:", msg);
    }
  });

  // ===============================
  // 6. 디버깅용
  // ===============================
  window._gameSocket = {
    socket,
    send,
  };
})();