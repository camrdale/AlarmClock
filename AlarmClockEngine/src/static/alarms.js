const crontabClassName = "crontabInput";
const buzzerClassName = "buzzerInput";

const defaultCrontab = "* * * * *";
const defaultBuzzer = false;

var numAlarmsDisplay = 10;

window.onload = function() {
    document.getElementById("more_alarms").addEventListener("click", moreAlarms);
    document.getElementById("show_claim_modal").addEventListener("click", showClaimModal);
    document.getElementById("close_claim_modal").addEventListener("click", closeClaimModal);
    document.getElementById("cancel_claim_modal").addEventListener("click", closeClaimModal);
    document.getElementById("claim_clock").addEventListener("click", claimClock);
    // Get the modal dialogs
    var claim_modal = document.getElementById("claim_modal");

    // When the user clicks anywhere outside of the modal, close it
    window.onclick = function(event) {
        if (event.target == claim_modal) {
            claim_modal.style.display = "none";
        }
    }
    document.getElementById("close_edit_modal").addEventListener("click", closeEditModal);
    document.getElementById("cancel_edit_modal").addEventListener("click", closeEditModal);
    document.getElementById("edit_clock").addEventListener("click", editClock);
    // Get the modal dialogs
    var edit_modal = document.getElementById("edit_modal");

    // When the user clicks anywhere outside of the modal, close it
    window.onclick = function(event) {
        if (event.target == edit_modal) {
            edit_modal.style.display = "none";
        }
    }
    loadData({});
}

function showClaimModal(event) {
    document.getElementById("claim_modal").style.display = "block";
}

function closeClaimModal(event) {
    document.getElementById("claim_modal").style.display = "none";
}

function claimClock(event) {
    var verificationNumber = document.getElementById("verification_number");
    data = {"verification_number": verificationNumber.value};

    var url = "/claim";

    console.log("Sending data:", data);
    fetch(url, {
      method: "POST",
      credentials: "same-origin",
      body: JSON.stringify(data),
      headers: new Headers({
        "Content-Type": "application/json"
      })
    }).then(res => res.json())
    .catch(error => console.error("Error claiming clock:", error))
    .then(data => claimedClock(data));
}

function claimedClock(data) {
    console.log("Claimed clock:", data);
    document.getElementById("claim_modal").style.display = "none";
    loadData({});
}

function showEditModal(event) {
    var form = event.currentTarget;
    while (form.parentNode) {
        form = form.parentNode;
        if (form.tagName === "FORM")
            break;
    }
    document.getElementById("edit_clock_key").value = form.id;
    document.getElementById("edit_modal").style.display = "block";
}

function closeEditModal(event) {
    document.getElementById("edit_modal").style.display = "none";
}

function editClock(event) {
    var clockName = document.getElementById("edit_clock_name");
    var timeZone = document.getElementById("edit_time_zone");
    var clockKey = document.getElementById("edit_clock_key");
    data = {"name": clockName.value,
            "time_zone": timeZone.value,
            "clock_key": clockKey.value};

    var url = "/edit";

    console.log("Sending data:", data);
    fetch(url, {
      method: "POST",
      credentials: "same-origin",
      body: JSON.stringify(data),
      headers: new Headers({
        "Content-Type": "application/json"
      })
    }).then(res => res.json())
    .catch(error => console.error("Error editing clock:", error))
    .then(data => editedClock(data));
}

function editedClock(data) {
    console.log("Edited clock:", data);
    document.getElementById("edit_modal").style.display = "none";
    loadData({});
}

function deleteClock(event) {
    var form = event.currentTarget;
    while (form.parentNode) {
        form = form.parentNode;
        if (form.tagName === "FORM")
            break;
    }
    var clockKey = form.id;
    data = {"clock_key": clockKey};

    var url = "/delete";

    console.log("Sending data:", data);
    fetch(url, {
      method: "POST",
      credentials: "same-origin",
      body: JSON.stringify(data),
      headers: new Headers({
        "Content-Type": "application/json"
      })
    }).then(res => res.json())
    .catch(error => console.error("Error deleting clock:", error))
    .then(data => deletedClock(data));
}

function deletedClock(data) {
    console.log("Deleted clock:", data);
    loadData({});
}

function saveAlarms(event) {
    var form = event.currentTarget;
    while (form.parentNode) {
        form = form.parentNode;
        if (form.tagName === "FORM")
            break;
    }
    var clock_key = form.id;

    var ulAlarms = form.getElementsByTagName("ul")[0];
    var alarms = [];
    
    var crontabs = ulAlarms.getElementsByClassName(crontabClassName);
    for (var i = 0; i < crontabs.length; i++) {
        var crontab = crontabs[i];
        var buzzer = crontab.parentNode.getElementsByClassName(buzzerClassName)[0];
        alarms.push({"crontab": crontab.value, "buzzer": buzzer.checked});
    }
    loadData({
        "clocks": [
            {
                "clock_key": clock_key,
                "new_alarms": alarms},
        ]});
}

function loadData(data) {
    var url = "/data";
    data["num_alarms_display"] = numAlarmsDisplay;

    console.log("Sending data:", data);
    fetch(url, {
      method: "POST",
      credentials: "same-origin",
      body: JSON.stringify(data),
      headers: new Headers({
        "Content-Type": "application/json"
      })
    }).then(res => handleJsonResponse(res))
    .catch(error => console.error("Error loading data:", error))
    .then(data => updateData(data));
}

function handleJsonResponse(response) {
    console.log('ok:', response.ok);
    console.log('status:', response.status);
    console.log('Headers:', response.headers);
    console.log('response:', response);
    if (response.ok) {
        return response.json();
    }
    if (response.status == 302) {
        console.log('Headers:', response.headers);
        var location = response.headers.get("location");
        console.log("302 Location:", location)
        window.location.href = location;
    }
    throw new Error('Network response was not ok.');
}

function updateData(data) {
    console.log("Loaded data:", data);

    var divClocks = document.getElementById("clocks");
    // Remove any existing data.
    while (divClocks.firstChild) {
        divClocks.removeChild(divClocks.firstChild);
    }

    var clocks = data["clocks"];
    for (var j = 0; j < clocks.length; j++) {
        var clock = clocks[j];

        var clockForm = document.createElement("form");
        clockForm.id = clock["clock_key"];
        
        var clockDescription = document.createElement("p");
        clockDescription.innerText = "Current alarms for clock " + clock["name"]
            + " in timezone " + clock["time_zone"];
        clockForm.appendChild(clockDescription);

        var ulAlarms = document.createElement("ul");
        var alarms = clock["alarms"];
        for (var i = 0; i < alarms.length; i++) {
            var alarm = alarms[i]
            ulAlarms.appendChild(createAlarmRow(alarm["crontab"], alarm["buzzer"]));
        }
        clockForm.appendChild(ulAlarms);
        
        var editButton = document.createElement("input");
        editButton.type = "button";
        editButton.value = "Edit Clock";
        editButton.addEventListener("click", showEditModal);
        clockForm.appendChild(editButton);
        
        var deleteButton = document.createElement("input");
        deleteButton.type = "button";
        deleteButton.value = "Delete Clock";
        deleteButton.addEventListener("click", deleteClock);
        clockForm.appendChild(deleteButton);
        
        var appendAlarmButton = document.createElement("input");
        appendAlarmButton.type = "button";
        appendAlarmButton.value = "Add Alarm";
        appendAlarmButton.addEventListener("click", appendAlarmRow);
        clockForm.appendChild(appendAlarmButton);
        
        var saveButton = document.createElement("input");
        saveButton.type = "button";
        saveButton.value = "Save Alarms";
        saveButton.addEventListener("click", saveAlarms);
        clockForm.appendChild(saveButton);
        
        divClocks.appendChild(clockForm);
    }
    
    var currentTime = document.getElementById("current_time");
    currentTime.innerHTML = data["now"]
    
    var numAlarmsDisplay = document.getElementById("num_alarms_display");
    numAlarmsDisplay.innerHTML = data["num_alarms_display"]

    var ulNextAlarms = document.getElementById("next_alarms");
    while (ulNextAlarms.firstChild) {
        ulNextAlarms.removeChild(ulNextAlarms.firstChild);
    }
    var nextAlarms = data["next_alarms"];
    for (var i = 0; i < nextAlarms.length; i++) {
        var li = document.createElement("li");
        li.innerHTML = nextAlarms[i];
        ulNextAlarms.appendChild(li);
    }
}

function appendAlarmRow(event) {
    var form = event.currentTarget;
    while (form.parentNode) {
        form = form.parentNode;
        if (form.tagName === "FORM")
            break;
    }
    var ul = form.getElementsByTagName("ul")[0];
    ul.appendChild(createAlarmRow(defaultCrontab, defaultBuzzer));
}

function createAlarmRow(crontab, buzzer) {
    var li = document.createElement("li");
    
    var text = document.createElement("input");
    text.classList.add(crontabClassName);
    text.type = "text";
    text.value = crontab;
    li.appendChild(text);
    
    var checkbox = document.createElement("input");
    checkbox.classList.add(buzzerClassName);
    checkbox.type = "checkbox";
    li.appendChild(checkbox);
    checkbox.checked = buzzer;

    var button = document.createElement("input");
    button.type = "button";
    button.value = "Delete Alarm";
    button.addEventListener("click", deleteAlarmRow);
    li.appendChild(button);
    
    return li;
}

function deleteAlarmRow(event) {
    var li = event.currentTarget;
    while (li.parentNode) {
        li = li.parentNode;
        if (li.tagName === "LI")
            break;
    }
    li.parentNode.removeChild(li);
}

function moreAlarms(event) {
    numAlarmsDisplay += 10;
    loadData({});
    event.currentTarget.scrollIntoView();
}
