(function () {
  "use strict";

  // 0. WebSocket 연결

  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";
  const socket = new WebSocket(socketUrl);

  function log() {
    const args = Array.prototype.slice.call(arguments);
    args.unshift("[WS midResult]");
    console.log.apply(console, args);
  }

  function send(message) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    } else {
      log("socket not open, cannot send", message);
    }
  }

  // 1. roomId / myName 메타 태그에서 읽기

  const roomMeta = document.querySelector('meta[name="room-id"]');
  const nameMeta = document.querySelector('meta[name="my-name"]');
  const roomId = roomMeta ? roomMeta.content : null;
  const myName = nameMeta ? nameMeta.content : null;

  // 2. WebSocket 이벤트 핸들러

  function onSocketOpen() {
    log("connected");

    if (roomId && myName) {
      send({
        type: "JOIN",
        roomId: roomId,
        nickname: myName,
        role: ""
      });
    } else {
      log("JOIN not sent - missing roomId or myName", {
        roomId: roomId,
        myName: myName
      });
    }
  }

  function onSocketClose() {
    log("closed");
  }

  function onSocketError(e) {
    log("error", e);
  }

  socket.addEventListener("open", onSocketOpen);
  socket.addEventListener("close", onSocketClose);
  socket.addEventListener("error", onSocketError);

  // 3. "다음 라운드" 버튼 클릭 처리

  const btn = document.getElementById("next-round-btn");

  function onNextRoundButtonClick() {
    if (!roomId || !myName) {
      log("NEXT_ROUND not sent - missing roomId or myName", {
        roomId: roomId,
        myName: myName
      });
      return;
    }

    send({
      type: "NEXT_ROUND",
      roomId: roomId,
      nickname: myName
    });
  }

  if (btn) {
    btn.addEventListener("click", onNextRoundButtonClick);
  }

  // 4. NEXT_ROUND 수신 시 처리

  // 기존 폼이 있으면 그걸 쓰고, 없으면 새로 만듦
  function getNextRoundForm() {
    const existingForm = document.getElementById("next-round-form");
    if (existingForm) {
      return existingForm;
    }

    const f = document.createElement("form");
    f.method = "post";
    f.action = "/game/next-round";
    f.id = "next-round-form";
    document.body.appendChild(f);
    return f;
  }

  function onSocketMessage(event) {
    let msg;
    try {
      msg = JSON.parse(event.data);
    } catch (e) {
      log("invalid json", event.data);
      return;
    }

    if (msg.type === "NEXT_ROUND") {
      if (window.location.pathname !== "/game/answer") {
        return;
      }

      const form = getNextRoundForm();
      form.submit();
    }
  }

  socket.addEventListener("message", onSocketMessage);

  // 5. 디버깅용 전역 객체

  window._midResultSocket = {
    socket: socket,
    send: send
  };
})();
