
class WebSocketClient {

    constructor(url, options = {}) {
        this.url = url;
        this.options = {
            onMessage: options.onMessage || this._defaultMessageHandler,
            onOpen: options.onOpen || this._defaultOpenHandler,
            onClose: options.onClose || this._defaultCloseHandler,
            onError: options.onError || this._defaultErrorHandler,
            autoReconnect: options.autoReconnect !== false, // Default to true
            reconnectInterval: options.reconnectInterval || 3000,
            maxReconnectAttempts: options.maxReconnectAttempts || 10
        };

        this.ws = null;
        this.reconnectAttempts = 0;
        this.reconnectTimeout = null;
        this.isConnecting = false;
        this.isManualClose = false;

        // Initialize the connection
        this.connect();
    }

    connect() {
        if (this.ws && (this.ws.readyState === WebSocket.CONNECTING ||
            this.ws.readyState === WebSocket.OPEN)) {
            console.log(`Already connected to ${this.url}`);
            return;
        }

        this.isConnecting = true;
        this.isManualClose = false;

        try {
            console.log(`Connecting to ${this.url}`);
            this.ws = new WebSocket(this.url);

            // Define WebSocket event handlers
            this.ws.onopen = () => {
                this.isConnecting = false;
                this.reconnectAttempts = 0;

                // Call the user's onOpen handler without any parameters
                this.options.onOpen();
            };

            this.ws.onmessage = (event) => {
                // Pass just the data to the onMessage handler, not the whole event
                this.options.onMessage(event.data);
            };

            this.ws.onclose = (event) => {
                this.isConnecting = false;

                if (this.options.onClose) {
                    this.options.onClose(event);
                }

                if (this.options.autoReconnect && !this.isManualClose) {
                    this._scheduleReconnect();
                }
            };

            this.ws.onerror = (error) => {
                if (this.options.onError) {
                    this.options.onError(error);
                }
            };

        } catch (error) {
            console.error('WebSocket connection error:', error);
            this._scheduleReconnect();
        }
    }

    send(data) {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.error('Cannot send: WebSocket is not connected');
            return false;
        }

        try {
            const message = typeof data === 'object' ? JSON.stringify(data) : data;
            this.ws.send(message);
            return true;
        } catch (error) {
            console.error('Error sending message:', error);
            return false;
        }
    }

    close(code, reason) {
        this.isManualClose = true;

        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        if (this.ws) {
            if (this.ws.readyState === WebSocket.OPEN) {
                this.ws.close(code, reason);
            }
        }
    }

    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }

    _scheduleReconnect() {
        if (this.isManualClose) return;

        if (this.reconnectAttempts < this.options.maxReconnectAttempts) {
            this.reconnectAttempts++;

            console.log(`Scheduling reconnect attempt ${this.reconnectAttempts}/${this.options.maxReconnectAttempts} in ${this.options.reconnectInterval}ms`);

            this.reconnectTimeout = setTimeout(() => {
                this.connect();
            }, this.options.reconnectInterval);
        } else {
            console.error(`Maximum reconnect attempts (${this.options.maxReconnectAttempts}) reached`);
        }
    }

    // Default handlers
    _defaultOpenHandler() {
        console.log(`WebSocket connected to ${this.url}`);
    }

    _defaultMessageHandler(data) {
        console.log('WebSocket message received:', data);
    }

    _defaultCloseHandler(event) {
        console.log(`WebSocket connection closed: Code=${event.code}, Reason=${event.reason}`);
    }

    _defaultErrorHandler(error) {
        console.error('WebSocket error:', error);
    }
}