// static/js/midresult-socket.js
(function () {
  // ===== 0. WebSocket 연결 =====
  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";
  const socket = new WebSocket(socketUrl);

  function log(...args) {
    console.log("[WS midResult]", ...args);
  }

  function send(message) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    } else {
      log("socket not open, cannot send", message);
    }
  }

  // ===== 1. roomId / myName meta에서 읽기 =====
  const roomMeta = document.querySelector('meta[name="room-id"]');
  const nameMeta = document.querySelector('meta[name="my-name"]');
  const roomId = roomMeta ? roomMeta.content : null;
  const myName = nameMeta ? nameMeta.content : null;

  // ===== 2. 소켓 연결되면 JOIN 한 번 보내서 방 매핑해두기 =====
  socket.addEventListener("open", () => {
    log("connected");

    if (roomId && myName) {
      send({
        type: "JOIN",
        roomId: roomId,
        nickname: myName,
        role: "", // 중간결과 페이지라 역할은 비워 둬도 됨
      });
    } else {
      log("JOIN not sent - missing roomId or myName", { roomId, myName });
    }
  });

  socket.addEventListener("close", () => log("closed"));
  socket.addEventListener("error", (e) => log("error", e));

  // ===== 3. 버튼 클릭 → 서버에 NEXT_ROUND 보내기 =====
  const btn = document.getElementById("next-round-btn");
  if (btn) {
    btn.addEventListener("click", () => {
      if (!roomId || !myName) {
        log("NEXT_ROUND not sent - missing roomId or myName", {
          roomId,
          myName,
        });
        return;
      }

      send({
        type: "NEXT_ROUND",
        roomId: roomId,
        nickname: myName,
      });
    });
  }

  // ===== 4. 서버에서 NEXT_ROUND 브로드캐스트 받으면 → 모두 POST /game/next-round =====
  socket.addEventListener("message", (event) => {
    let msg;
    try {
      msg = JSON.parse(event.data);
    } catch (e) {
      log("invalid json", event.data);
      return;
    }

    if (msg.type === "NEXT_ROUND") {
      // midResult(/game/answer)에서만 동작
      if (window.location.pathname !== "/game/answer") return;

      // 기존 form이 있으면 그걸, 없으면 새로 만들어서 submit
      const form =
        document.getElementById("next-round-form") ||
        (() => {
          const f = document.createElement("form");
          f.method = "post";
          f.action = "/game/next-round";
          document.body.appendChild(f);
          return f;
        })();

      form.submit();
    }
  });

  // 디버깅용
  window._midResultSocket = {
    socket,
    send,
  };
})();




//(function () {
//  const socketUrl =
//    (location.protocol === "https:" ? "wss://" : "ws://") +
//    location.host +
//    "/ws/game";
//  const socket = new WebSocket(socketUrl);
//
//  function log(...args) {
//    console.log("[WS midResult]", ...args);
//  }
//
//  socket.addEventListener("open", () => log("connected"));
//  socket.addEventListener("close", () => log("closed"));
//  socket.addEventListener("error", (e) => log("error", e));
//
//  function send(message) {
//    if (socket.readyState === WebSocket.OPEN) {
//      socket.send(JSON.stringify(message));
//    }
//  }
//
//  // 버튼 클릭하면 NEXT_ROUND 브로드캐스트
//  const btn = document.getElementById("next-round-btn");
//  if (btn) {
//    btn.addEventListener("click", (e) => {
//      e.preventDefault();
//      send({ type: "NEXT_ROUND" });
//    });
//  }
//
//  // 서버에서 NEXT_ROUND 받으면 모두 /game/next-round POST
//  socket.addEventListener("message", (event) => {
//    let msg;
//    try {
//      msg = JSON.parse(event.data);
//    } catch (e) {
//      log("invalid json", event.data);
//      return;
//    }
//
//    if (msg.type === "NEXT_ROUND") {
//      const form = document.createElement("form");
//      form.method = "post";
//      form.action = "/game/next-round";
//      document.body.appendChild(form);
//      form.submit();
//    }
//  });
//})();