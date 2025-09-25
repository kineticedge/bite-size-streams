
// Reference to the table body
const tableBody = document.querySelector('#commitTable tbody');

const subtopologyTable = document.querySelector('#subtopologyTable tbody');

const currentTime = document.querySelector('#currentTime');

// Store thread data for calculating "Time Since Last Commit"
const threadData = {};


// Fetch data from endpoint
fetch("/processes")
    .then(response => response.json()) // Parse the JSON response
    .then(data => {
        populateDropdown(data); // Populate the dropdown with the API response
    })
    .catch(error => {
        console.error('Error fetching data:', error);
    });
const dropdown = document.getElementById('dropdown');
// Function to populate the dropdown
function populateDropdown(data) {
    // Loop over the JSON data
    data.forEach(item => {
        // Create an <option> element
        const option = document.createElement('option');
        option.value = item.id;  // Use the 'id' as the value
        option.textContent = item.label;  // Use the 'label' as the display text
        // Append to the dropdown
        dropdown.appendChild(option);
    });
}


// let filterText = ''; // Stores the filter text entered by the user
//
// document.getElementById('applyFilter').addEventListener('click', () => {
//     // Get the filter text from the input
//     filterText = document.getElementById('filterInput').value;
//     console.log(`Filter applied: ${filterText}`);
// });

let form = document.getElementById('configForm');
form.addEventListener('submit', async (event) => {
    console.log("configForm");
    event.preventDefault(); // Prevent the default form submission
    const formData = new FormData(form);
    const data = {
        mode: 'normal',
        process: formData.get('dropdown'),
        age: formData.get('age'),
        types: formData.get('types')
    };
    try {
        // Send the form data to the server using fetch()
        const response = await fetch('/emit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        });
        console.log(response);
        const result = await response.text(); // Adjust based on your backend response format
        console.log(result);
        document.getElementById('response').innerText = result; // Display response
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('response').innerText = 'An error occurred, please try again.';
    }
});

let formPP = document.getElementById('poisonPill');
formPP.addEventListener('submit', async (event) => {
    event.preventDefault(); // Prevent the default form submission
    const formData = new FormData(formPP);
    const data = {
        mode: 'poison-pill'
    };
    try {
        // Send the form data to the server using fetch()
        const response = await fetch('/emit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        });
        console.log(response);
        const result = await response.text(); // Adjust based on your backend response format
        console.log(result);
        document.getElementById('response').innerText = result; // Display response
    } catch (error) {
        console.error('Error:', error);
        document.getElementById('response').innerText = 'An error occurred, please try again.';
    }
});



let form2 = document.getElementById('featureToggle');
form2.addEventListener('change', async (event) => {

    event.preventDefault();

    const toggleCheckbox = document.getElementById('featureToggle');
    const valueInput = document.getElementById('featureValue');

    console.log("featureToggle");

    try {
        // Send the form data to the server using fetch()
        const response = await fetch('/check', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                enabled: toggleCheckbox.checked,
                value: valueInput.value
            }),
        });
        console.log(response);
        const result = await response.text();
        console.log(result);
    } catch (error) {
        console.error('Error:', error);
    }
});

const threadRows = {};

// A function to update the table
function updateTable(data) {
    // Clear the current table rows
    tableBody.innerHTML = '';

    console.log('updateTable');

    //document.getElementById('applicationId').innerText = data.applicationId;

    const metadataTbody = document.getElementById('metadata')
    metadataTbody.innerHTML = '';
    const entries = Object.entries(data.metadata ?? {});
    // Optional: sort keys for stable order
//    entries.sort(([a], [b]) => a.localeCompare(b));
    entries.forEach(([key, value]) => {
        const tr = document.createElement('tr');
        const tdKey = document.createElement('td');
        tdKey.textContent = key;
        const tdValue = document.createElement('td');
        // For primitives and simple values
        tdValue.textContent = value === null ? 'null' : String(value);
        tr.appendChild(tdKey);
        tr.appendChild(tdValue);
        metadataTbody.appendChild(tr);
    });


    subtopologyTable.innerHTML = '';
    data.subtopology.forEach(subtopology => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${subtopology.id}</td>
            <td>${formatEpochToTime(subtopology.streamTimestamp)}</td>
        `;
        subtopologyTable.appendChild(row);
    });


    // Populate table with new data
    data.thread.forEach(thread => {
        const threadName = thread.name;
        const lastCommitTime = new Date(thread.commitTimestamp).getTime(); // Parse commit time

        // Calculate "Time Since Last Commit" based on threadData
        const timeSinceLastCommit = calculateTimeSinceLastCommit(lastCommitTime);

        //console.log(timeSinceLastCommit);

        // Update thread data for future reference
        threadData[threadName] = lastCommitTime;

        // Create a new row for the table
        // Add cells for Thread Name, Commit Time, and Time Since Last Commit
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${threadName}</td>
          <td>${formatEpochToTime(lastCommitTime)}<span class="time-since-commit" data-commit-time="${lastCommitTime}" style="font-size: smaller">(${timeSinceLastCommit}s)</span></td>
          
        `;
        const row2 = document.createElement('tr');
        row2.innerHTML = `
          <td></td>
          <td>${thread.tasks}</td>
        `;

        threadRows[threadName] = row.querySelector('.time-since-commit');


        // Append the row to the table body
        tableBody.appendChild(row);
        tableBody.appendChild(row2);
    });
}


// Function to update all "time since last commit" values
function updateTimeSinceCommit() {
    const now = Date.now();

    currentTime.innerText = formatEpochToTime(now);

    // Update each thread's "time since last commit" display
    Object.keys(threadRows).forEach(threadName => {
        const cell = threadRows[threadName];
        const commitTime = parseInt(cell.getAttribute('data-commit-time'));
        const timeSinceLastCommit = calculateTimeSinceLastCommit(commitTime, now).toFixed(1);
        cell.textContent = `(${timeSinceLastCommit}s)`;
    });
}

// // Function to calculate time since last commit
// function calculateTimeSinceLastCommit(commitTime, currentTime = Date.now()) {
//     const timeDifference = currentTime - commitTime;
//     return Math.floor(timeDifference / 1000); // Convert to seconds
// }

// Start a timer to update "time since last commit" every second
setInterval(updateTimeSinceCommit, 100);


// Initialize EventSource for persistent connection
const eventSource = new EventSource("/threads");

// Handle incoming messages from the server
eventSource.onmessage = (event) => {
    try {
        // Parse the data received from the server
        const data = JSON.parse(event.data);

        //console.log(data);

        // Update the table with the new data
        updateTable(data);
    } catch (error) {
        console.error('Error handling server message:', error);
    }
};

// Handle errors in the connection
eventSource.onerror = (error) => {
    console.error('Error with SSE connection:', error);

    // Optionally, close the connection if necessary
    // eventSource.close();
};

// Optional: Detect when the connection is opened
eventSource.onopen = () => {
    console.log('SSE connection opened.');
};


