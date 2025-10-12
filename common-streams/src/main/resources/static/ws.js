const WS_URL = 'ws://localhost:8081';

let activeSockets = {}; // To track open dialogs by topic

function ws_updateEventSourceDialog(topic, e) {


    // remove leading path...
    topic = topic.replace(/^\/[^\/]+\//, '')

    const isPartitionDialog = topic.match(/^.+\/[0-9]+$/) !== null;

    console.log("XXXXX_" + topic + "__" + isPartitionDialog);

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
                <div class="dialog-buttons"">
                    ${isPartitionDialog ? '' : `
                    <div class="partition-selector">
                        <button id="${topic}/0" class="partition-btn partition-0">0</button>
                        <button id="${topic}/1" class="partition-btn partition-1">1</button>
                    </div>
                    `}
                <span class="dialog-recycle-btn" style="background: none; border: none; cursor: pointer;">
                <svg viewBox="-0.5 -0.5 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" id="Rewind--Streamline-Solar" height="16" width="16">
                    <path d="M13.125 3.375v8.25c0 0.51525 -0.5625 0.82875 -0.984375 0.54675l-4.37625 -2.91375v2.36625c0 0.51525 -0.5625 0.82875 -0.984375 0.54675l-4.6875 -3.125c-0.375 -0.249375 -0.375 -0.8006250000000001 0 -1.05l4.6875 -3.125c0.421875 -0.282 0.984375 0.03075 0.984375 0.54675v2.36625l4.37625 -2.91375c0.421875 -0.282 0.984375 0.03075 0.984375 0.54675Z" transform="translate(-1.5, 0)" stroke-width="1"></path>
                    <path d="M2.5 0c0.55 0 1 0.45 1 1v13c0 0.55 -0.45 1 -1 1s-1 -0.45 -1 -1v-13c0 -0.55 0.45 -1 1 -1Z" stroke-width="1"></path>
                </svg>

<!--                    <svg viewBox="-0.5 -0.5 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" id="Rewind&#45;&#45;Streamline-Solar" height="16" width="16">-->
<!--                        <path d="M13.125 3.375v8.25c0 0.51525 -0.5625 0.82875 -0.984375 0.54675l-4.37625 -2.91375v2.36625c0 0.51525 -0.5625 0.82875 -0.984375 0.54675l-4.6875 -3.125c-0.375 -0.249375 -0.375 -0.8006250000000001 0 -1.05l4.6875 -3.125c0.421875 -0.282 0.984375 0.03075 0.984375 0.54675v2.36625l4.37625 -2.91375c0.421875 -0.282 0.984375 0.03075 0.984375 0.54675Z" stroke-width="1"></path>-->
<!--                        <path d="M2.34375 3.375c0 -0.51525 0.421875 -0.9375 0.9375 -0.9375s0.9375 0.421875 0.9375 0.9375v8.25c0 0.51525 -0.421875 0.9375 -0.9375 0.9375s-0.9375 -0.421875 -0.9375 -0.9375v-8.25Z" stroke-width="1"></path>-->
<!--                    </svg>-->
                </span>
                <span class="dialog-clear-btn" style="background: none; border: none; cursor: pointer;">
                    <svg viewBox="-0.5 -0.5 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" id="Eraser--Streamline-Solar" height="16" width="16">
                        <path d="M7.130999999999999 3.4406625C8.17475 2.3968875 8.696625000000001 1.875 9.345125 1.875c0.6485625 0 1.1704375 0.5218875 2.2141875 1.5656625C12.603124999999999 4.484437499999999 13.125 5.006331250000001 13.125 5.65485c0 0.6485249999999999 -0.521875 1.1704 -1.5656875000000001 2.21415l-2.62075 2.6208125 -4.42835 -4.4283875 2.6207875000000005 -2.6207625Z" fill="#ffffff" stroke-width="1"></path>
                        <path d="m3.8473125 6.724375 4.428375 4.4283125000000005 -0.4066875 0.40662499999999996c-0.23606249999999998 0.23612500000000003 -0.4454375 0.4455 -0.6356249999999999 0.6281875000000001H13.125c0.258875 0 0.46875 0.20987499999999998 0.46875 0.46875s-0.20987499999999998 0.46875 -0.46875 0.46875H5.625c-0.63485625 -0.0158125 -1.156625 -0.5379375 -2.1843375000000003 -1.5656875000000001C2.3968875 10.5155625 1.875 9.9936875 1.875 9.345125c0 -0.6485000000000001 0.5218875 -1.170375 1.5656625 -2.214125l0.40665 -0.40662499999999996Z" fill="#ffffff" stroke-width="1"></path>
                    </svg>
                </span>
                </div>
            </div>
            <div class="scrollable-table">
            </div>
            <div class="dialog-handle"></div>
        `;

    makeDialogDraggable(dialog, dialog.querySelector('.dialog-header'));
    makeDialogResizable(dialog, dialog.querySelector('.dialog-handle'));

    const partition0Btn = dialog.querySelector(".partition-0");
    if (partition0Btn) {
        partition0Btn.onclick = () => {
            ws_updateEventSourceDialog("/events/" + partition0Btn.id, e);
            // setActivePartitionButton(dialog, partition0Btn);
            // const separator = dialog.dataset.baseUrl.includes('?') ? '&' : '?';
            // loadTableData(dialog, `${dialog.dataset.baseUrl}${separator}partition=0`);
        };
    }

    const partition1Btn = dialog.querySelector(".partition-1");
    if (partition1Btn) {
        partition1Btn.onclick = () => {
            ws_updateEventSourceDialog("/events/" + partition1Btn.id, e);
            // setActivePartitionButton(dialog, partition1Btn);
            // const separator = dialog.dataset.baseUrl.includes('?') ? '&' : '?';
            // loadTableData(dialog, `${dialog.dataset.baseUrl}${separator}partition=1`);
        };
    }


    document.body.appendChild(dialog);

    dialog.querySelector(".dialog-close-btn").onclick = () => {
        //e.preventDefault();
        // Close the dialog and stop the SSE connection
        dialog.remove();
        console.log("WS close: topic=" + topic);
        if (activeSockets[topic].ws) {
            activeSockets[topic].ws.close();
        }
        delete activeSockets[topic];
    };

    dialog.querySelector(".dialog-clear-btn").onclick = () => {
        const tbody = dialog.querySelector(".tbody");
        while (tbody.firstChild) {
            tbody.removeChild(tbody.firstChild);
        }
    };

    dialog.querySelector(".dialog-recycle-btn").onclick = () => {
        const tbody = dialog.querySelector(".tbody");
        tbody.innerHTML = ``
        if (ws && ws.isConnected()) {
            ws.send('_rewind');
            console.log('Sent _rewind command to WebSocket');
        } else {
            console.error('WebSocket not connected, cannot send _rewind command');
        }
    };


    dialog.querySelector(".scrollable-table").innerHTML = `
                <table class="table" style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr class="sticky-header">
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

    const ws = new WebSocketClient(WS_URL, {
        // send: () => {
        //     ws.send("X");
        // },
        onOpen: () => {
            console.log("WS onOpen: topic=" + topic);
            //TODO
            //ws.send(topic);
            ws.send(topic);
        },
        onMessage: (message) => {
            const data = JSON.parse(message);
            const newRow = document.createElement("tr");
            newRow.className = "prevent-move";
            newRow.innerHTML = `
                    <td>${data.key}</td>
                    <td>${data.partition}</td>
                    <td>${data.offset}</td>
                    <td>${formatISODate(new Date(data.timestamp).toISOString())}</td>
                    <td>${data.type}</td>
                `;
            const newRow2 = document.createElement("tr");

            if (data.datatype === "JSON") {

                const jsonObject = JSON.parse(data.value);

                const keys = Object
                    .keys(jsonObject)
                    .filter(key => key !== '_type' && key !== 'iteration' && key !== 'ts');

                //TODO make sure doesn't break
                delete jsonObject._type;

                newRow2.className = "prevent-move";
                newRow2.innerHTML = `
                        <td colspan="5">
                          <pre class="json-newspaper">${customJsonStringify(jsonObject, 2)}</pre>
                        </td>
                    `;

                // const headersHTML = keys.map(key => `<th>${key}</th>`).join('');
                // const valuesHTML = keys.map(key => {
                //     const value = jsonObject[key];
                //     if (value === null) {
                //         return `<td><em>null</em></td>`;
                //     } else if (typeof value === 'object') {
                //         return `<td>${customJsonStringify(value, 2)}</td>`;
                //     } else {
                //         return `<td>${value}</td>`;
                //     }
                // }).join('');
                //
                // newRow2.innerHTML = `
                //     <td colspan="5">
                //         <table class="json-table">
                //             <thead>
                //                 <tr>${headersHTML}</tr>
                //             </thead>
                //             <tbody>
                //                 <tr>${valuesHTML}</tr>
                //             </tbody>
                //         </table>
                //     </td>
                // `;

            } else if (data.datatype === "XML") {
                newRow2.className = "prevent-move";
                newRow2.innerHTML = `
                            <td colspan="5">
                              <pre class="xml-newspaper">${escapeXML(data.value)}</pre>
                            </td>
                    `;
            } else {
                console.log(">>");
                console.log(`>> ${data.value}`)
                newRow2.className = "prevent-move";
                newRow2.innerHTML = `
                            <td colspan="5">
                              ${data.value}
                            </td>
                    `;
            }

            const tbody = dialog.querySelector(".tbody");
            tbody.appendChild(newRow);
            tbody.appendChild(newRow2);

            if (tbody.rows.length > 1000) {
                tbody.deleteRow(0);
                tbody.deleteRow(0);
            }

            const tc = dialog.querySelector('.scrollable-table');
           tc.scrollTop = tc.scrollHeight;

            // // Create scroll tracker for this dialog
            // dialog.scrollTracker = createScrollTracker(tc);
            //
            // // Add a method to the dialog to scroll to bottom when needed
            // dialog.scrollToBottom = () => {
            //     scrollToBottom(tc);
            // };
            //
            // // Always scroll to bottom initially
            // setTimeout(() => dialog.scrollToBottom(), 0);

        }
    })

    activeSockets[topic] = {dialog, ws}; // Track this dialog and EventSource

    document.querySelector('.scrollable-table').addEventListener('wheel', (event) => {
        const tableContainer = event.currentTarget;

        // Check if currently at the top or bottom of the scrollable table
        const isAtTop = tableContainer.scrollTop === 0;
        //const isAtBottom = tableContainer.scrollTop + tableContainer.clientHeight >= tableContainer.scrollHeight;
        const isAtBottom = Math.abs(tableContainer.scrollHeight - tableContainer.clientHeight - tableContainer.scrollTop) < 1;

        // Allow scrolling inside the table, but prevent scrolling the page when at top/bottom
        if ((isAtTop && event.deltaY < 0) || (isAtBottom && event.deltaY > 0)) {
            event.preventDefault(); // Prevent the event from propagating to the page
        }
    });
}

