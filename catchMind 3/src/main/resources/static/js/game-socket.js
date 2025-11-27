// static/js/game-socket.js

(function () {
  // ===============================
  // 0. 공통: WebSocket 연결
  // ===============================
  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";            // <- 서버에서 만든 엔드포인트에 맞추기
  const socket = new WebSocket(socketUrl);

  function log(...args) {
    console.log("[WS]", ...args);
  }

  socket.addEventListener("open", () => log("connected"));
  socket.addEventListener("close", () => log("closed"));
  socket.addEventListener("error", (e) => log("error", e));

  function send(message) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    } else {
      log("socket not open, cannot send:", message);
    }
  }

  // 내 역할(DRAWER / GUESSER)
  const roleMeta = document.querySelector('meta[name="my-role"]');
  const myRole = roleMeta ? roleMeta.content : null;

  // ===============================
  // 1. 캔버스 기본 세팅
  // ===============================
  const canvas = document.getElementById("drawing-canvas");
  const ctx = canvas ? canvas.getContext("2d") : null;

  if (canvas && ctx) {
    // 부트스트랩 ratio-4x3 안에 있으니까 실제 표시 크기에 맞게 조정
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

  // 실제 선 그리기
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

  // ===============================
  // 2. DRAWER: 마우스로 그리기 + 서버로 좌표 전송
  //   JSON 형식 :
  //   {
  //     "type": "DRAW",
  //     "x": 120,
  //     "y": 240,
  //     "color": "#000000",
  //     "thickness": 4,
  //     "dragging": true
  //   }
  // ===============================
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
      x: x,
      y: y,
      color: "#000000",
      thickness: 4,
      dragging: dragging, // true면 이전 점과 이어지는 선
    });
  }

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

      // 내 화면에는 바로 선 그리기
      drawLine(lastX, lastY, pos.x, pos.y, "#000000", 4);

      // 서버로 현재 점 전송 (이전 점과 이어지는 선)
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
  // 3. DRAW 메시지 수신 처리 (Drawer & Guesser 둘 다)
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

    // dragging === true 이고 이전 점이 있으면 선으로 연결
    if (dragging && hasPrevPoint) {
      drawLine(prevX, prevY, x, y, color, thickness);
    } else {
      // 새 스트로크 시작점: 점 하나만 찍기 (원 안 채우기)
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
  // 4. GUESSER: 정답 전송
  // ===============================
  const guessForm = document.getElementById("guess-form");
  const answerInput = document.getElementById("answer-input");

  if (guessForm && answerInput) {
    guessForm.addEventListener("submit", (e) => {
      e.preventDefault();

      const value = answerInput.value.trim();
      if (!value) return;

      send({
        type: "GUESS",
        guess: value,
      });

      answerInput.value = "";
      answerInput.focus();
    });
  }

  // ===============================
  // 5. 라운드/점수/타이머 & 결과 UI
  // ===============================

  // 네잎클로버 아이콘 렌더링
  function renderAttempts(triesLeft) {
    const container = document.getElementById("attempt-icons");
    if (!container) return;

    container.innerHTML = "";
    for (let i = 0; i < triesLeft; i++) {
      const img = document.createElement("img");
      img.src = "/image/clover_icon.png"; // 정적 경로 확인 필요
      img.alt = "Attempt";
      img.style.height = "18px";
      img.style.margin = "0 2px";
      container.appendChild(img);
    }
  }

  function handleGuessResult(msg) {
    const resultDiv = document.getElementById("guess-result");
    if (resultDiv) {
      resultDiv.textContent = msg.correct
        ? "정답입니다!"
        : "틀렸습니다. 다시 시도해보세요!";
      resultDiv.className =
        "mt-3 fw-semibold " + (msg.correct ? "text-success" : "text-danger");
    }

    if (typeof msg.triesLeft === "number") {
      renderAttempts(msg.triesLeft);
    }

    const scoreSpan = document.getElementById("score");
    if (scoreSpan && typeof msg.totalScore === "number") {
      scoreSpan.textContent = msg.totalScore;
    }

    const roundSpan = document.getElementById("round-info");
    if (roundSpan && typeof msg.round === "number") {
      roundSpan.textContent = msg.round + " / 6";
    }
  }

  let timerInterval = null;

  function startTimer(limitSeconds, serverStartTime) {
    if (timerInterval) clearInterval(timerInterval);

    timerInterval = setInterval(() => {
      const nowSec = Date.now() / 1000;
      const elapsed = nowSec - serverStartTime;
      const remain = Math.max(0, limitSeconds - elapsed);

      const min = Math.floor(remain / 60);
      const sec = Math.floor(remain % 60);

      const display = document.getElementById("timer-display");
      if (display) {
        display.textContent =
          String(min).padStart(2, "0") + ":" + String(sec).padStart(2, "0");
      }

      if (remain <= 0) {
        clearInterval(timerInterval);
        timerInterval = null;
      }
    }, 250);
  }

  function handleRoundStart(msg) {
    const roundSpan = document.getElementById("round-info");
    if (roundSpan && typeof msg.round === "number") {
      roundSpan.textContent = msg.round + " / 6";
    }

    if (typeof msg.triesLeft === "number") {
      renderAttempts(msg.triesLeft);
    }

    const scoreSpan = document.getElementById("score");
    if (scoreSpan && typeof msg.totalScore === "number") {
      scoreSpan.textContent = msg.totalScore;
    }

    // Drawer에게만 제시어 표시
    if (myRole === "DRAWER") {
      const wordEl = document.getElementById("drawer-word");
      if (wordEl && msg.answerForDrawer) {
        wordEl.textContent = msg.answerForDrawer;
      }
    }

    clearCanvas();
    hasPrevPoint = false;

    if (
      typeof msg.limitSeconds === "number" &&
      typeof msg.serverStartTime === "number"
    ) {
      startTimer(msg.limitSeconds, msg.serverStartTime);
    }
  }

  function handleRoundEnd(msg) {
      // JS로 POST form 만들어서 자동 submit  (라운드 끝나고 이동 오류 해결)
      const form = document.createElement("form");
      form.method = "POST";
      form.action = "/game/next-round";

      document.body.appendChild(form);
      form.submit();
  }

  function handleGameOver(msg) {
    window.location.href = "/game/final";
  }

  // ===============================
  // 6. WebSocket 메시지 분기 처리
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

      case "ROUND_START":
        handleRoundStart(msg);
        break;

      case "ROUND_END":
        handleRoundEnd(msg);
        break;

      case "GAME_OVER":
        handleGameOver(msg);
        break;

      default:
        log("unknown message type:", msg);
    }
  });

  // ===============================
  // 7. 디버깅용 전역 훅
  // ===============================
  window.gameTest = { handleGuessResult, handleRoundStart, renderAttempts };
})();
