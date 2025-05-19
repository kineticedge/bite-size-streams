
// used for dialogs to ensure they are on the top of other dialogs when created,
// also used by mouse down on a dialog to raise the dialog above others.
let zIndexCounter = 1000


function makeDialogDraggable(dialog, header) {

    let offsetX = 0, offsetY = 0;

    header.addEventListener("mousedown", (event) => {
        event.preventDefault();
        dialog.style.zIndex = (zIndexCounter++).toString();
        offsetX = event.clientX - dialog.getBoundingClientRect().left;
        offsetY = event.clientY - dialog.getBoundingClientRect().top;
        document.addEventListener("mousemove", onMouseMove);
        document.addEventListener("mouseup", onMouseUp);
    });

    function onMouseMove(event) {
        event.preventDefault();
        dialog.style.left = `${event.clientX - offsetX}px`;
        dialog.style.top = `${event.clientY - offsetY}px`;
    }

    function onMouseUp(event) {
        event.preventDefault();
        document.removeEventListener("mousemove", onMouseMove);
        document.removeEventListener("mouseup", onMouseUp);
    }
}

function makeDialogResizable(dialog, handle) {

    let startWidth, startHeight, startX, startY;

    handle.addEventListener('mousedown', (event) => {
        event.preventDefault();

        const rect = dialog.getBoundingClientRect();
        startWidth = rect.width;
        startHeight = rect.height;
        startX = event.clientX;
        startY = event.clientY;

        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    });

    function onMouseMove(event) {
        event.preventDefault();
        dialog.style.width = `${startWidth + (event.clientX - startX)}px`;
        dialog.style.height = `${startHeight + (event.clientY - startY)}px`;
    }

    function onMouseUp(event) {
        event.preventDefault();
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
    }
}

function formatEpochToTime(epochMillis) {
    // Convert epoch milliseconds to a Date object
    const date = new Date(epochMillis);

    // Extract hours, minutes, seconds, and milliseconds
    const hours = date.getHours().toString().padStart(2, '0'); // Ensures 2-digit format
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');
    const milliseconds = date.getMilliseconds().toString().padStart(3, '0'); // Ensure 3-digit format

    // Construct the formatted string
    return `${hours}:${minutes}:${seconds}.${milliseconds}`;
    //return `${minutes}:${seconds}.${milliseconds}`;
}

function escapeXML(xml) {
    return xml
        .replace(/&/g, "&amp;")   // Escape ampersand
        .replace(/</g, "&lt;")   // Escape less than
        .replace(/>/g, "&gt;")   // Escape greater than
        .replace(/'/g, "&apos;") // Escape single quote
        .replace(/"/g, "&quot;"); // Escape double quote
}

function formatXML(xmlString) {
    try {
        // Parse the XML string into a DOM object
        const parser = new DOMParser();
        const xmlDocument = parser.parseFromString(xmlString, "application/xml");

        // Check for parsing errors
        const parseError = xmlDocument.getElementsByTagName("parsererror");
        if (parseError.length) {
            console.error("Invalid XML:", parseError[0].textContent);
            return null;
        }

        // Use XMLSerializer to serialize the DOM back to a string
        const serializer = new XMLSerializer();
        const rawXML = serializer.serializeToString(xmlDocument);

        // Beautify - Add line breaks and indentation
        const formatted = rawXML.replace(/(>)(<)(\/*)/g, "$1\n$2$3"); // Add line breaks
        const lines = formatted.split("\n");
        let indentLevel = 0;

        return lines
            .map((line) => {
                if (line.match(/<\/\w/)) indentLevel--; // Closing tag, decrease indent
                const indent = "  ".repeat(indentLevel);
                if (line.match(/<\w[^>]*[^\/]>.*$/)) indentLevel++; // Opening tag, increase indent
                return indent + line;
            })
            .join("\n");
    } catch (error) {
        console.error("Error formatting XML:", error);
        return null;
    }
}


// A function to calculate time difference
function calculateTimeSinceLastCommit(lastCommitTime) {
    const now = new Date().getTime(); // Current time in milliseconds
    return Math.floor((now - lastCommitTime) / 100) / 10; // Return difference in seconds
}
