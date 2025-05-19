async function fetchDotAndRender() {

    const container = document.getElementById('graph-container');
    if (container.clientHeight <= 20) {
        setTimeout(fetchDotAndRender, 50); // Try again in 50ms
        return;
    }

    console.log("fetchDotAndRender");

    const response = await fetch('/topology');
    if (!response.ok) {
        throw new Error('Failed to fetch DOT format from server');
    }
    const dot = await response.text();

    const graph = d3.select("#graph");


    console.log(container.clientHeight + " : " + container.clientWidth);

    d3.select("#graph")
        .graphviz()
        .width(container.clientWidth)
        .height(container.clientHeight)
        .fit(true)
        .renderDot(dot).on("end", () => {
        // Add JavaScript event listeners after rendering is complete
        overrideTopicLinks();
    });
}

//fetchDotAndRender();
window.addEventListener('resize', debounce(fetchDotAndRender, 250));

// Debounce function to prevent too many resize events
function debounce(func, wait) {
    let timeout;
    return function() {
        const context = this;
        const args = arguments;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
}

function initialRender() {
    const container = document.getElementById('graph-container');

    // Wait until container has a meaningful height
    if (container.clientHeight <= 100) {
        setTimeout(initialRender, 50);
        return;
    }

    // Now render with proper dimensions
    //resizeGraph();
    fetchDotAndRender();
}

// Start the initial rendering process
initialRender();


function overrideTopicLinks() {

    console.log("overrideTopicLinks");

    // Select all <a> tags inside the graph
    const links = document.querySelectorAll("#graph a[*|href]");

    // Loop over each <a> tag
    links.forEach((link) => {

        console.log(link);

        const href = link.getAttribute("href");

        // If the link is an SSE URL, override its behavior
        if (href && href.startsWith("/events/")) {
            link.addEventListener("click", (e) => {
                e.preventDefault(); // Stop default navigation behavior
                console.log(`Updating SSE connection to: ${href}`);

                // Update the EventSource connection dynamically
                //updateEventSource(href);
                ws_updateEventSourceDialog(href, e);
            });
        } else if (href && href.startsWith("/stores/")) {
            link.addEventListener("click", (e) => {
                e.preventDefault(); // Stop default navigation behavior
                updateStateStoreDialog(href);
            });
        }
    });
}
