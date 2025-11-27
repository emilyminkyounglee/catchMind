// static/js/game-socket.js

(function () {
  // 1) WebSocket 연결
  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";
  const socket = new WebSocket(socketUrl);

  // 내 역할(DRAWER / GUESSER) 읽기 - meta에서 가져옴
  const roleMeta = document.querySelector('meta[name="my-role"]');
  const myRole = roleMeta ? roleMeta.content : null;

  // 공통 캔버스
  const canvas = document.getElementById("drawing-canvas");
  const ctx = canvas ? canvas.getContext("2d") : null;

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

  // ===============================
  // 2. DRAWER용: 그림 그리기 → 서버로 전송
  // ===============================
  let drawing = false;

  function drawPoint(x, y) {
    if (!ctx) return;
    ctx.fillStyle = "#000000";
    ctx.beginPath();
    ctx.arc(x, y, 2, 0, Math.PI * 2);
    ctx.fill();
  }

  if (canvas && ctx && myRole === "DRAWER") {
    function getPos(e) {
      const rect = canvas.getBoundingClientRect();
      return {
        x: e.clientX - rect.left,
        y: e.clientY - rect.top,
      };
    }

    canvas.addEventListener("mousedown", (e) => {
      drawing = true;
      const { x, y } = getPos(e);
      drawPoint(x, y); // 내 화면
      send({ type: "DRAW", x, y });
    });

    canvas.addEventListener("mousemove", (e) => {
      if (!drawing) return;
      const { x, y } = getPos(e);
      drawPoint(x, y); // 내 화면
      send({ type: "DRAW", x, y });
    });

    window.addEventListener("mouseup", () => {
      drawing = false;
    });
  }

  // ===============================
  // 3. 메시지 수신 처리
  // ===============================
  socket.addEventListener("message", (event) => {
    let msg;
    try {
      msg = JSON.parse(event.data);
    } catch (e) {
      log("invalid json", event.data);
      return;
    }

    // type에 따라 분기
    switch (msg.type) {
      case "DRAW":
        ...
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
        log("unknown type", msg);
    }
  });

  // ===============================
  // 4. GUESSER: 정답 입력 → WebSocket 전송
  // ===============================
  const guessForm = document.getElementById("guess-form");
  const answerInput = document.getElementById("answer-input");

  if (guessForm && answerInput) {
    guessForm.addEventListener("submit", (e) => {
      e.preventDefault();

      const value = answerInput.value.trim();
      if (!value) return;

      // GUESS 메시지 전송
      send({
        type: "GUESS",
        guess: value,
      });

      // 입력칸 초기화 + 포커스 유지
      answerInput.value = "";
      answerInput.focus();
    });
  }

  // ===============================
  // 5. GUESS_RESULT, ROUND_START UI 업데이트
  // ===============================
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
    // 결과 텍스트
    const resultDiv = document.getElementById("guess-result");
    if (resultDiv) {
      resultDiv.textContent = msg.correct
        ? "정답입니다!"
        : "틀렸습니다. 다시 시도해보세요!";
      resultDiv.className =
        "mt-3 fw-semibold " + (msg.correct ? "text-success" : "text-danger");
    }

    // 남은 시도 (네잎클로버)
    if (typeof msg.triesLeft === "number") {
      renderAttempts(msg.triesLeft);
    }

    // 점수 갱신
    const scoreSpan = document.getElementById("score");
    if (scoreSpan && typeof msg.totalScore === "number") {
      scoreSpan.textContent = msg.totalScore;
    }

    // 라운드 정보(옵션)
    const roundSpan = document.getElementById("round-info");
    if (roundSpan && typeof msg.round === "number") {
      roundSpan.textContent = msg.round + " / 6";
    }
  }

  function handleRoundStart(msg) {
    // 라운드 정보
    const roundSpan = document.getElementById("round-info");
    if (roundSpan && typeof msg.round === "number") {
      roundSpan.textContent = msg.round + " / 6";
    }

    // 시도 횟수 초기화
    if (typeof msg.triesLeft === "number") {
      renderAttempts(msg.triesLeft);
    }

    // 점수 갱신
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

    // 타이머 시작 (서버가 limitSeconds, serverStartTime 보내준다고 가정)
    if (typeof msg.limitSeconds === "number" && typeof msg.serverStartTime === "number") {
      startTimer(msg.limitSeconds, msg.serverStartTime);
    }
  }

//  타이머 함수 추가
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

function handleRoundEnd(msg) {
  // 나중에 필요하면 msg.round / msg.roundScore 등 저장해도 됨
  window.location.href = `/game/mid?round=${msg.round}`;
}

function handleGameOver(msg) {
  window.location.href = "/game/final";
}

  // ✅ 테스트용 전역 훅 (IIFE 안, 함수 밖)
  window.gameTest = { handleGuessResult, handleRoundStart, renderAttempts };
})();

