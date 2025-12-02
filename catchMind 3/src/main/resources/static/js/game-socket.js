// static/js/game-socket.js

(function () {
  // ===============================
  // 0. 공통: WebSocket 연결
  // ===============================


  // 내 역할(DRAWER / GUESSER)
  const roleMeta = document.querySelector('meta[name="my-role"]');
  const myRole = roleMeta ? roleMeta.content : null;
   // [ADD] 방/닉네임 정보 (JOIN, DRAW 때 같이 보낼 것)
    const roomMeta = document.querySelector('meta[name="room-id"]');
    const nameMeta = document.querySelector('meta[name="my-name"]');
    const roomId = roomMeta ? roomMeta.content : null;
    const myName = nameMeta ? nameMeta.content : null;


  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";            // <- 서버에서 만든 엔드포인트에 맞추기
  const socket = new WebSocket(socketUrl);

  function log(...args) {
    console.log("[WS]", ...args);
  }


  //socket.addEventListener("open", () => log("connected"));
    socket.addEventListener("open", () => {
      log("connected");

      // [ADD] 연결되면 JOIN 메시지 한번 날려주기
      if (roomId && myName) {
        send({
          type: "JOIN",
          roomId: roomId,
          nickname: myName,
          role: myRole, // DRAWER / GUESSER
        });
      } else {
        log("JOIN not sent - roomId or myName missing", { roomId, myName, myRole });
      }
    });
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
//  // ===============================
  const guessForm = document.getElementById("guess-form");
  const answerInput = document.getElementById("answer-input");
   if (guessForm && answerInput) {
      guessForm.addEventListener("submit", (e) => {
        e.preventDefault(); // ★ 여기서 페이지 이동 막기

        const value = answerInput.value.trim();
        if (!value) return;

        const formData = new FormData(guessForm);
        // answer, userId 둘 다 form 안에 있으니까 그대로 전송
        fetch(guessForm.action, {
          method: "POST",
          body: formData,
        })
          .then((res) => {
            // 굳이 응답 HTML 안 써도 됨
            // round가 끝나면 서버에서 ROUND_END 메시지 보내고,
            // game-socket.js의 handleRoundEnd가 /game/answer 로 이동시켜 줌
          })
          .catch((err) => {
            console.error("submit error", err);
          });

        // 입력창은 비우고 다시 포커스
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

  // ===============================
  // 5. 라운드/점수/타이머 & 결과 UI
  // ===============================

  /**
   * GUESS_RESULT 메시지 처리
   * - Drawer/Guesser 둘 다 이걸 받아서 "정답/오답" 메시지를 공유해서 볼 수 있게 함
   */
      /**
       * GUESS_RESULT 메시지 처리
       * - Drawer/Guesser 둘 다 이걸 받아서 "정답/오답" + 시도횟수/점수/라운드 공유
       */
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

        // ---- 여기서부터 공통 UI 동기화 ----

        // Attempts(네잎클로버)
        if (typeof msg.triesLeft === "number") {
          renderAttempts(msg.triesLeft);
        }

        // Score
        if (typeof msg.totalScore === "number") {
          const scoreSpan = document.getElementById("score");
          if (scoreSpan) {
            scoreSpan.textContent = msg.totalScore;
          }
        }

        // Round
        if (typeof msg.round === "number") {
          const roundSpan = document.getElementById("round-info");
          if (roundSpan) {
            roundSpan.textContent = msg.round + " / 6";
          }
        }
      }

  function handleRoundEnd(msg) {
    window.location.href = "/game/answer";  // GET 요청 → 위 @GetMapping 타서 midResult 렌더
  }
  function handleRoundNext(msg) {
   // 내가 아직 midResult(/game/answer)에 있다면
    // 자동으로 /game/next-round POST 해서 다음 라운드로 진입
    if (window.location.pathname === "/game/answer") {
      const form = document.createElement("form");
      form.method = "post";
      form.action = "/game/next-round";
      document.body.appendChild(form);
      form.submit();
    }
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
      case "ROUND_END":
        handleRoundEnd(msg);
        break;
      case "ROUND_NEXT":
        handleRoundNext(msg);
        break;

      default:
        log("unknown message type:", msg);
    }
  });

  // ===============================
  // 7. 디버깅용 전역 훅
  // ===============================
})();
