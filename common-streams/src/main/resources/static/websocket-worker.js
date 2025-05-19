// websocket-worker.js
let socket = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000; // 3 seconds

// Handle messages from the main thread
self.onmessage = function(event) {
    const { cmd, data, topic } = event.data;

    switch(cmd) {
        case 'connect':
            connectWebSocket(data.url, topic);
            break;
        case 'send':
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(data.message);
            } else {
                self.postMessage({ type: 'error', message: 'Socket not connected', topic });
            }
            break;
        case 'close':
            if (socket) {
                socket.close();
                socket = null;
            }
            break;
    }
};

function connectWebSocket(wsUrl, topic) {
    try {
        if (socket) {
            socket.close();
        }

        socket = new WebSocket(wsUrl);

        socket.onopen = function() {
            reconnectAttempts = 0;
            self.postMessage({ type: 'open', topic });

            // Send the topic subscription request once connected
            if (topic) {
                socket.send(topic);
            }
        };

        socket.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                self.postMessage({ type: 'message', data, topic });
            } catch (e) {
                self.postMessage({
                    type: 'error',
                    message: 'Error parsing message: ' + e.message,
                    data: event.data,
                    topic
                });
            }
        };

        socket.onerror = function(error) {
            self.postMessage({ type: 'error', message: 'WebSocket error', topic });
        };

        socket.onclose = function(event) {
            self.postMessage({ type: 'close', code: event.code, reason: event.reason, topic });

            // Attempt to reconnect if the close wasn't clean
            if (!event.wasClean && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                reconnectAttempts++;
                setTimeout(() => {
                    self.postMessage({ type: 'reconnecting', attempt: reconnectAttempts, topic });
                    connectWebSocket(wsUrl, topic);
                }, RECONNECT_DELAY);
            }
        };
    } catch (error) {
        self.postMessage({ type: 'error', message: error.message, topic });
    }
}