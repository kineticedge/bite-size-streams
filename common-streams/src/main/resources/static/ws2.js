const WS_URL = 'ws://localhost:8081';
let activeSockets = {}; // To track open dialogs by topic
//let zIndexCounter = 1000; // Make sure this is defined or imported from common.js

function ws_updateEventSourceDialog(topic, e) {
    // remove leading path...
    topic = topic.replace(/^\/[^\/]+\//, '');

    console.log("WS updateEventSourceDialog: topic=" + topic);

    // If a dialog for the topic is already open, focus on it
    if (activeSockets[topic]) {
        activeSockets[topic].dialog.focus();
        return;
    }

    const dialog = document.createElement("div");
    dialog.style.left = `${e.clientX}px`;
    dialog.style.top = `${e.clientY}px`;
    dialog.style.zIndex = (zIndexCounter++).toString();
    dialog.className = "dialog";
    dialog.innerHTML = `
            <div class="dialog-header">
                <div class="dialog-close-btn"></div>
                <div class="dialog-title">${topic}</div>
                <div class="dialog-clear-btn" style="background: none; border: none; cursor: pointer;">
                    <svg viewBox="-0.5 -0.5 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" id="Eraser--Streamline-Solar" height="16" width="16">
                        <path d="M7.130999999999999 3.4406625C8.17475 2.3968875 8.696625000000001 1.875 9.345125 1.875c0.6485625 0 1.1704375 0.5218875 2.2141875 1.5656625C12.603124999999999 4.484437499999999 13.125 5.006331250000001 13.125 5.65485c0 0.6485249999999999 -0.521875 1.1704 -1.5656875000000001 2.21415l-2.62075 2.6208125 -4.42835 -4.4283875 2.6207875000000005 -2.6207625Z" fill="#ffffff" stroke-width="1"></path>
                        <path d="m3.8473125 6.724375 4.428375 4.4283125000000005 -0.4066875 0.40662499999999996c-0.23606249999999998 0.23612500000000003 -0.4454375 0.4455 -0.6356249999999999 0.6281875000000001H13.125c0.258875 0 0.46875 0.20987499999999998 0.46875 0.46875s-0.20987499999999998 0.46875 -0.46875 0.46875H5.625c-0.63485625 -0.0158125 -1.156625 -0.5379375 -2.1843375000000003 -1.5656875000000001C2.3968875 10.5155625 1.875 9.9936875 1.875 9.345125c0 -0.6485000000000001 0.5218875 -1.170375 1.5656625 -2.214125l0.40665 -0.40662499999999996Z" fill="#ffffff" stroke-width="1"></path>
                    </svg>
                </div>
            </div>
            <div class="scrollable-table">
            </div>
            <div class="dialog-handle"></div>
        `;

    makeDialogDraggable(dialog, dialog.querySelector('.dialog-header'));
    makeDialogResizable(dialog, dialog.querySelector('.dialog-handle'));

    document.body.appendChild(dialog);

    dialog.querySelector(".scrollable-table").innerHTML = `
                <table class="table" style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr>
                            <th>key</th>
                            <th>partition</th>
                            <th>offset</th>
                            <th>timestamp</th>
                            <th>data type</th>
                        </tr>
                    </thead>
                    <tbody class="tbody"></tbody>
                </table>
        `;

    // Create a web worker
    const worker = new Worker('websocket-worker.js');

    // Set up worker message handling
    worker.onmessage = function(event) {
        const { type, data, message, topic: responseTopic } = event.data;

        switch(type) {
            case 'open':
                console.log(`WebSocket connection opened for ${responseTopic}`);
                break;

            case 'message':
                // Process the incoming WebSocket message
                const tbody = dialog.querySelector(".tbody");
                const newRow = document.createElement("tr");

                // Add table cells with the data
                newRow.innerHTML = `
                    <td>aaa ${data.key}</td>
                    <td>${data.partition}</td>
                    <td>${data.offset}</td>
                    <td>${new Date(data.timestamp).toLocaleTimeString()}</td>
                    <td>${data.type}</td>
                `;


                const newRow2 = document.createElement("tr");

                if (data.datatype === "JSON") {
                    const jsonObject = JSON.parse(data.value);
                    newRow2.innerHTML = `
                        <td colspan="5">
                          <pre class="json-newspaper">${JSON.stringify(jsonObject, null, 2)}</pre>
                        </td>
                    `;
                } else if (data.datatype === "XML") {
                    newRow2.innerHTML = `
                            <td colspan="5">
                              ${escapeXML(data.value)}
                            </td>
                    `;
                } else {
                    console.log(">>");
                    console.log(`>> ${data.value}`)
                    newRow2.innerHTML = `
                            <td colspan="5">
                              UKDT = ${data.value}
                            </td>
                    `;
                }

                //const tbody = dialog.querySelector(".tbody");
                tbody.appendChild(newRow);
                tbody.appendChild(newRow2);

                const MAX = 200;

                if (tbody.rows.length > MAX) {
                    tbody.deleteRow(0);
                    tbody.deleteRow(0);
                }

                const tc = document.querySelector('.scrollable-table');
                tc.scrollTop = tc.scrollHeight;




                // tbody.appendChild(newRow);
                //
                // // Auto-scroll to show the newest message
                // const scrollable = dialog.querySelector(".scrollable-table");
                // scrollable.scrollTop = scrollable.scrollHeight;

                break;

            case 'error':
                console.error(`WebSocket error for ${responseTopic}:`, message);
                break;

            case 'close':
                console.log(`WebSocket closed for ${responseTopic}`);
                break;

            case 'reconnecting':
                console.log(`Attempting to reconnect WebSocket (${event.data.attempt})`);
                break;
        }
    };

    // Store the worker and dialog in the activeSockets object
    activeSockets[topic] = {
        worker: worker,
        dialog: dialog
    };

    // Connect to the WebSocket server with the topic
    worker.postMessage({
        cmd: 'connect',
        data: { url: WS_URL },
        topic: topic
    });

    // Setup close button handler
    dialog.querySelector(".dialog-close-btn").onclick = () => {
        dialog.remove();
        console.log("WS close: topic=" + topic);

        if (activeSockets[topic]) {
            // Tell the worker to close the connection
            activeSockets[topic].worker.postMessage({
                cmd: 'close',
                topic: topic
            });

            // Terminate the worker
            activeSockets[topic].worker.terminate();
            delete activeSockets[topic];
        }
    };

    // Setup clear button handler
    dialog.querySelector(".dialog-clear-btn").onclick = () => {
        const tbody = dialog.querySelector(".tbody");
        while (tbody.firstChild) {
            tbody.removeChild(tbody.firstChild);
        }
    };
}