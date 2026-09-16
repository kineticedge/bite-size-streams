
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

        // Bind methods to ensure 'this' context
        this._handleOpen = this._handleOpen.bind(this);
        this._handleMessage = this._handleMessage.bind(this);
        this._handleClose = this._handleClose.bind(this);
        this._handleError = this._handleError.bind(this);

        // Initialize the connection
        this.connect();
    }

    /**
     * Connect to the WebSocket server
     */
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

            this.ws.onopen = this._handleOpen;
            this.ws.onmessage = this._handleMessage;
            this.ws.onclose = this._handleClose;
            this.ws.onerror = this._handleError;
        } catch (error) {
            console.error('WebSocket connection error:', error);
            this._scheduleReconnect();
        }
    }

    /**
     * Send data to the WebSocket server
     * @param {string|Object} data - Data to send (objects will be JSON.stringify'd)
     * @returns {boolean} - Whether the send was successful
     */
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

    /**
     * Close the WebSocket connection
     * @param {number} code - Close code (optional)
     * @param {string} reason - Close reason (optional)
     */
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

    /**
     * Check if the WebSocket is currently connected
     * @returns {boolean} - Connection status
     */
    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }

    // Private handler methods
    _handleOpen(event) {
        this.isConnecting = false;
        this.reconnectAttempts = 0;
        this.options.onOpen(event, this);
    }

    _handleMessage(event) {
        this.options.onMessage(event, this);
    }

    _handleClose(event) {
        this.isConnecting = false;
        this.options.onClose(event, this);

        if (this.options.autoReconnect && !this.isManualClose) {
            this._scheduleReconnect();
        }
    }

    _handleError(error) {
        this.options.onError(error, this);
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
    _defaultOpenHandler(event, client) {
        console.log(`WebSocket connected to ${client.url}`);
    }

    _defaultMessageHandler(event, client) {
        console.log('WebSocket message received:', event.data);
    }

    _defaultCloseHandler(event, client) {
        console.log(`WebSocket connection closed: Code=${event.code}, Reason=${event.reason}`);
    }

    _defaultErrorHandler(error, client) {
        console.error('WebSocket error:', error);
    }
}