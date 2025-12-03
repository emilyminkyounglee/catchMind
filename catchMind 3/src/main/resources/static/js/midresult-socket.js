// static/js/midresult-socket.js
(function () {
  "use strict";

  // ===============================
  // 0. WebSocket 연결
  //  - /ws/game 엔드포인트에 WebSocket을 연결한다.
  //  - log / send 공통 유틸 함수를 제공한다.
  // ===============================

  const socketUrl =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/ws/game";
  const socket = new WebSocket(socketUrl);

  // 콘솔 로그 유틸리티
  function log() {
    const args = Array.prototype.slice.call(arguments);
    args.unshift("[WS midResult]");
    console.log.apply(console, args);
  }

  // JSON 메시지 전송 유틸리티
  function send(message) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    } else {
      log("socket not open, cannot send", message);
    }
  }

  // ===============================
  // 1. roomId / myName 메타 태그에서 읽기
  //  - 서버에서 템플릿에 넣어준 meta 태그를 읽어서
  //    방 번호와 내 닉네임을 확보한다.
  // ===============================

  const roomMeta = document.querySelector('meta[name="room-id"]');
  const nameMeta = document.querySelector('meta[name="my-name"]');
  const roomId = roomMeta ? roomMeta.content : null;
  const myName = nameMeta ? nameMeta.content : null;

  // ===============================
  // 2. WebSocket 이벤트 핸들러
  //  - open: JOIN 한 번 보내서 방 매핑
  //  - close / error: 디버깅 로그
  // ===============================

  function onSocketOpen() {
    log("connected");

    if (roomId && myName) {
      send({
        type: "JOIN",
        roomId: roomId,
        nickname: myName,
        role: "" // 중간 결과 페이지라 역할은 비워 둬도 됨
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

  // ===============================
  // 3. "다음 라운드" 버튼 클릭 처리
  //  - 클릭 시 서버에 NEXT_ROUND WebSocket 메시지를 보낸다.
  //  - roomId 또는 myName이 없으면 전송하지 않는다.
  // ===============================

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

  // ===============================
  // 4. NEXT_ROUND 수신 시 처리
  //  - 서버에서 NEXT_ROUND를 브로드캐스트하면
  //    midResult(/game/answer) 페이지에서만
  //    POST /game/next-round 로 폼 제출을 수행한다.
  // ===============================

  // 기존 폼이 있으면 그걸 쓰고, 없으면 새로 만드는 함수
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
      // /game/answer 페이지일 때만 동작
      if (window.location.pathname !== "/game/answer") {
        return;
      }

      const form = getNextRoundForm();
      form.submit();
    }
  }

  socket.addEventListener("message", onSocketMessage);

  // ===============================
  // 5. 디버깅용 전역 객체
  //  - 콘솔에서 window._midResultSocket.socket / send 로 확인 가능
  // ===============================

  window._midResultSocket = {
    socket: socket,
    send: send
  };
})();
