
// used for dialogs to ensure they are on the top of other dialogs when created,
// also used by mouse down on a dialog to raise the dialog above others.
let zIndexCounter = 1000


// function makeDialogDraggable(dialog, header) {
//
//     let offsetX = 0, offsetY = 0;
//
//     header.addEventListener("mousedown", (event) => {
//         event.preventDefault();
//         dialog.style.zIndex = (zIndexCounter++).toString();
//         offsetX = event.clientX - dialog.getBoundingClientRect().left;
//         offsetY = event.clientY - dialog.getBoundingClientRect().top;
//         document.addEventListener("mousemove", onMouseMove);
//         document.addEventListener("mouseup", onMouseUp);
//     });
//
//     function onMouseMove(event) {
//         event.preventDefault();
//         dialog.style.left = `${event.clientX - offsetX}px`;
//         dialog.style.top = `${event.clientY - offsetY}px`;
//     }
//
//     function onMouseUp(event) {
//         event.preventDefault();
//         document.removeEventListener("mousemove", onMouseMove);
//         document.removeEventListener("mouseup", onMouseUp);
//     }
// }
//
// function makeDialogResizable(dialog, handle) {
//
//     let startWidth, startHeight, startX, startY;
//
//     handle.addEventListener('mousedown', (event) => {
//         event.preventDefault();
//
//         const rect = dialog.getBoundingClientRect();
//         startWidth = rect.width;
//         startHeight = rect.height;
//         startX = event.clientX;
//         startY = event.clientY;
//
//         document.addEventListener('mousemove', onMouseMove);
//         document.addEventListener('mouseup', onMouseUp);
//     });
//
//     function onMouseMove(event) {
//         event.preventDefault();
//         dialog.style.width = `${startWidth + (event.clientX - startX)}px`;
//         dialog.style.height = `${startHeight + (event.clientY - startY)}px`;
//     }
//
//     function onMouseUp(event) {
//         event.preventDefault();
//         document.removeEventListener('mousemove', onMouseMove);
//         document.removeEventListener('mouseup', onMouseUp);
//     }
// }

function makeDialogDraggable(dialog) {
    // Make the entire dialog draggable (not just the header)
    let offsetX = 0, offsetY = 0;
    let isDragging = false;
    let hasMoved = false;

    // Raise dialog on any click
    dialog.addEventListener("mousedown", (event) => {
        // Raise dialog z-index on any click
        dialog.style.zIndex = (zIndexCounter++).toString();

        // Skip dragging behavior if the target is a button or handle
        const target = event.target;
        if (target.tagName === 'INPUT' ||
            target.tagName === 'BUTTON' ||
            target.tagName === 'PRE' ||
            target.closest('.dialog-buttons') ||
            target.closest('.dialog-close-btn') ||
            target.closest('.dialog-clear-btn') ||
            target.closest('.dialog-recycle-btn') ||
            target.closest('.dialog-handle') ||
            target.closest('.dialog-corner') ||
            target.closest('.prevent-move') ||
            target.closest('.partition-selector')) {
            return;
        }

        event.preventDefault();
        isDragging = true;
        hasMoved = false;
        offsetX = event.clientX - dialog.getBoundingClientRect().left;
        offsetY = event.clientY - dialog.getBoundingClientRect().top;
        document.addEventListener("mousemove", onMouseMove);
        document.addEventListener("mouseup", onMouseUp);
    });

    function onMouseMove(event) {
        if (!isDragging) return;
        event.preventDefault();
        dialog.style.left = `${event.clientX - offsetX}px`;
        dialog.style.top = `${event.clientY - offsetY}px`;
    }

    function onMouseUp() {
        isDragging = false;
        document.removeEventListener("mousemove", onMouseMove);
        document.removeEventListener("mouseup", onMouseUp);
    }
}

function makeDialogResizable(dialog) {
    // Add resize handles to all corners and edges
    const positions = ['n', 's', 'e', 'w', 'ne', 'se', 'sw', 'nw'];

    // First remove the existing handle if present
    const existingHandle = dialog.querySelector('.dialog-handle');
    if (existingHandle) {
        existingHandle.remove();
    }

    // Create corner elements
    positions.forEach(pos => {
        const handle = document.createElement('div');
        handle.className = `dialog-corner dialog-${pos}`;
        handle.style.position = 'absolute';

        // Set position and appearance based on the corner/edge
        switch(pos) {
            case 'n':
                handle.style.top = '-5px';
                handle.style.left = '0';
                handle.style.width = '100%';
                handle.style.height = '10px';
                handle.style.cursor = 'n-resize';
                break;
            case 'ne':
                handle.style.top = '-5px';
                handle.style.right = '-5px';
                handle.style.width = '10px';
                handle.style.height = '10px';
                handle.style.cursor = 'ne-resize';
                break;
            case 'e':
                handle.style.top = '0';
                handle.style.right = '-5px';
                handle.style.width = '10px';
                handle.style.height = '100%';
                handle.style.cursor = 'e-resize';
                break;
            case 'se':
                handle.style.bottom = '-5px';
                handle.style.right = '-5px';
                handle.style.width = '10px';
                handle.style.height = '10px';
                handle.style.cursor = 'se-resize';
                break;
            case 's':
                handle.style.bottom = '-5px';
                handle.style.left = '0';
                handle.style.width = '100%';
                handle.style.height = '10px';
                handle.style.cursor = 's-resize';
                break;
            case 'sw':
                handle.style.bottom = '-5px';
                handle.style.left = '-5px';
                handle.style.width = '10px';
                handle.style.height = '10px';
                handle.style.cursor = 'sw-resize';
                break;
            case 'w':
                handle.style.top = '0';
                handle.style.left = '-5px';
                handle.style.width = '10px';
                handle.style.height = '100%';
                handle.style.cursor = 'w-resize';
                break;
            case 'nw':
                handle.style.top = '-5px';
                handle.style.left = '-5px';
                handle.style.width = '10px';
                handle.style.height = '10px';
                handle.style.cursor = 'nw-resize';
                break;
        }

        dialog.appendChild(handle);

        // Add resize functionality to each handle
        let startWidth, startHeight, startX, startY, startLeft, startTop;

        handle.addEventListener('mousedown', (event) => {
            event.preventDefault();
            event.stopPropagation(); // Prevent dialog dragging

            // Raise dialog z-index on resize attempt too
            dialog.style.zIndex = (zIndexCounter++).toString();

            const rect = dialog.getBoundingClientRect();
            startWidth = rect.width;
            startHeight = rect.height;
            startLeft = rect.left;
            startTop = rect.top;
            startX = event.clientX;
            startY = event.clientY;

            document.addEventListener('mousemove', handleResize);
            document.addEventListener('mouseup', stopResize);
        });

        function handleResize(event) {
            event.preventDefault();

            // Calculate position deltas
            const dx = event.clientX - startX;
            const dy = event.clientY - startY;

            // Apply resizing based on the handle position
            if (pos.includes('e')) {
                dialog.style.width = `${startWidth + dx}px`;
            }
            if (pos.includes('s')) {
                dialog.style.height = `${startHeight + dy}px`;
            }
            if (pos.includes('w')) {
                dialog.style.width = `${startWidth - dx}px`;
                dialog.style.left = `${startLeft + dx}px`;
            }
            if (pos.includes('n')) {
                dialog.style.height = `${startHeight - dy}px`;
                dialog.style.top = `${startTop + dy}px`;
            }
        }

        function stopResize() {
            document.removeEventListener('mousemove', handleResize);
            document.removeEventListener('mouseup', stopResize);
        }
    });
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

// remove year to save space, also remove "Z" suffix
function formatISODate(isoString) {
    return isoString.replace(/Z$/, '').replace(/^\d{4}-/, '');
}

function customJsonStringify(obj, spaces = 2) {
    // First, stringify the object normally
    let jsonStr = JSON.stringify(obj, null, spaces);

    // Then replace arrays containing only 2 numbers with a compact format
    return jsonStr.replace(/\[\s*(\d+),\s*(\d+)\s*\]/g, '[$1,$2]');
}


/**
 * Scrolls a container to the bottom
 * @param {HTMLElement} container - The container element to scroll
 * @param {boolean} [smooth=false] - Whether to use smooth scrolling
 */
function scrollToBottom(container, smooth = false) {
    // Check if container exists
    if (!container) return;

    // Scroll to bottom
    if (smooth) {
        container.scrollTo({
            top: container.scrollHeight,
            behavior: 'smooth'
        });
    } else {
        container.scrollTop = container.scrollHeight;
    }
}

/**
 * Creates a function that maintains scroll position relative to the bottom
 * @param {HTMLElement} container - The scrollable container
 * @returns {Function} A function to call before adding content
 */
function createScrollTracker(container) {
    let shouldScrollToBottom = true;

    // Set up scroll event to detect if user has manually scrolled up
    container.addEventListener('scroll', () => {
        // If we're near the bottom (within 20px), we should auto-scroll
        const isNearBottom = Math.abs(
            (container.scrollHeight - container.clientHeight) - container.scrollTop
        ) < 20;

        shouldScrollToBottom = isNearBottom;
    });

    return {
        // Call before adding content to decide if we should scroll after
        shouldScroll: () => shouldScrollToBottom,

        // Call after adding content to scroll if needed
        scrollIfNeeded: () => {
            if (shouldScrollToBottom) {
                scrollToBottom(container);
            }
        }
    };
}