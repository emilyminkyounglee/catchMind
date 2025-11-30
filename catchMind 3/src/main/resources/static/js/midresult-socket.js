(function () {
  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";
  const socket = new WebSocket(socketUrl);

  function log(...args) {
    console.log("[WS midResult]", ...args);
  }

  socket.addEventListener("open", () => log("connected"));
  socket.addEventListener("close", () => log("closed"));
  socket.addEventListener("error", (e) => log("error", e));

  function send(message) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    }
  }

  // 버튼 클릭하면 NEXT_ROUND 브로드캐스트
  const btn = document.getElementById("next-round-btn");
  if (btn) {
    btn.addEventListener("click", (e) => {
      e.preventDefault();
      send({ type: "NEXT_ROUND" });
    });
  }

  // 서버에서 NEXT_ROUND 받으면 모두 /game/next-round POST
  socket.addEventListener("message", (event) => {
    let msg;
    try {
      msg = JSON.parse(event.data);
    } catch (e) {
      log("invalid json", event.data);
      return;
    }

    if (msg.type === "NEXT_ROUND") {
      const form = document.createElement("form");
      form.method = "post";
      form.action = "/game/next-round";
      document.body.appendChild(form);
      form.submit();
    }
  });
})();