

async function loadTableData(dialog, url) {
    // Clear the table first
    dialog.querySelector('.tbody').innerHTML = '';

    try {
        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error(`Error: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();

        //TODO FILTER

        const tbody = dialog.querySelector(".tbody");
        for (let i = 0; i < data.length; i++) {
            const newRow = document.createElement("tr");
            newRow.innerHTML = `
                <td> ${data[i].key}</td>
                <td>${data[i]?.timestamp ? formatISODate(new Date(data[i].timestamp).toISOString()) : '<em>n/a</em>'}</td>
                <td>${data[i].type}</td>
            `;

            const newRow2 = document.createElement("tr");

            newRow2.innerHTML = `
                    <td colspan="3">
                      <pre class="json-newspaper">${customJsonStringify(data[i].value, 2)}</pre>
                    </td>
                `;

            // if (data.datatype === "JSON") {
            //     const jsonObject = JSON.parse(data[i].value);
            //     delete jsonObject._type;
            //     newRow2.innerHTML = `
            //             <td colspan="3">
            //               <pre class="json-newspaper">${customJsonStringify(jsonObject, 2)}</pre>
            //             </td>
            //         `;
            // } else if (data.datatype === "XML") {
            //     newRow2.innerHTML = `
            //                 <td colspan="3">
            //                   <pre class="xml-newspaper">${escapeXML(data[i].value)}</pre>
            //                 </td>
            //         `;
            // } else {
            //     newRow2.innerHTML = `
            //                 <td colspan="3">
            //                   ${data[i].value}
            //                 </td>
            //         `;
            // }

            tbody.appendChild(newRow);
            tbody.appendChild(newRow2);
        }
    } catch (error) {
        console.error("Failed to load data:", error);
        // Optionally show error message in the UI
    }
}


async function updateStateStoreDialog(url) {
    try {

        const element = document.createElement("div");
        element.innerHTML = `
            <table class="table" style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr class="sticky-header">
                        <th>key</th>
                        <th>timestamp</th>
                        <th>data type</th>
<!--                        <th>value</th>-->
                    </tr>
                </thead>
                <tbody class="tbody"></tbody>
            </table>
            `;

        const dialog = createDialog(
            0,
            0,
            url,
            element,
            () => {
                console.log("CLOSE");
            }
        );

        loadTableData(dialog, url);
    } catch (error) {
        console.error("Failed to fetch data:", error);
    }
}




function createDialog(x, y, title, content, onclose) {

    const dialog = document.createElement("div");
    dialog.className = "dialog";
    dialog.style.left = `${x}px`;
    dialog.style.top = `${y}px`;
    dialog.style.zIndex = (zIndexCounter++).toString();
    dialog.innerHTML = `
            <div class="dialog-header">
                <div class="dialog-close-btn"></div>
                <div class="dialog-title">${title}</div>
                <div class="dialog-buttons">
                    <span>
                        <input type="checkbox" id="refreshToggle" class="dialog-auto-refresh"/>
                    </span>
                    <span class="dialog-clear-btn">
                        <svg viewBox="-0.5 -0.5 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" height="16" width="16">
                            <path d="M13.125 3.375v8.25c0 0.51525 -0.5625 0.82875 -0.984375 0.54675l-4.37625 -2.91375v2.36625c0 0.51525 -0.5625 0.82875 -0.984375 0.54675l-4.6875 -3.125c-0.375 -0.249375 -0.375 -0.8006250000000001 0 -1.05l4.6875 -3.125c0.421875 -0.282 0.984375 0.03075 0.984375 0.54675v2.36625l4.37625 -2.91375c0.421875 -0.282 0.984375 0.03075 0.984375 0.54675Z" transform="translate(-1.5, 0)" stroke-width="1"></path>
                            <path d="M2.5 0c0.55 0 1 0.45 1 1v13c0 0.55 -0.45 1 -1 1s-1 -0.45 -1 -1v-13c0 -0.55 0.45 -1 1 -1Z" stroke-width="1"></path>
                        </svg>
                    </span>
                </div>
            </div>
            <div class="scrollable-table">
            </div>
            <div class="dialog-handle"></div>
        `;

    let refreshTimerId = null;

    dialog.querySelector(".dialog-auto-refresh").onclick = () => {
        if (dialog.querySelector(".dialog-auto-refresh").checked) {
            console.log("Auto-refresh is enabled");

            // If already scheduled, clear it before scheduling again
            if (refreshTimerId !== null) {
                clearInterval(refreshTimerId);
            }
            loadTableData(dialog, title);
            refreshTimerId = setInterval(() => loadTableData(dialog, title), 5000);

        } else {
            console.log("Auto-refresh is disabled");
            if (refreshTimerId !== null) {
                clearInterval(refreshTimerId);
                refreshTimerId = null;
            }
        }
    };

    dialog.querySelector(".dialog-close-btn").onclick = () => {
        if (refreshTimerId !== null) {
            clearInterval(refreshTimerId);
            refreshTimerId = null;
        }
        dialog.remove();
        onclose();
    };

    dialog.querySelector(".dialog-clear-btn").onclick = async () => {
        loadTableData(dialog, title);
    };

    document.body.appendChild(dialog);

    dialog.querySelector('.scrollable-table')
        .appendChild(content);

    makeDialogDraggable(dialog, dialog.querySelector('.dialog-header'));
    makeDialogResizable(dialog, dialog.querySelector('.dialog-handle'));

    return dialog;
}
