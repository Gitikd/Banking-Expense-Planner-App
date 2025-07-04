const token = localStorage.getItem("jwt");

// Load banking summary
async function loadSummary() {
	const res = await fetch("/api/banking/summary", {
		headers: { Authorization: "Bearer " + token }
	});
	const data = await res.json();
	document.getElementById("userName").innerText = data.user;
	document.getElementById("balance").innerText = "₹ " + data.totalBalance.toLocaleString("en-IN", { minimumFractionDigits: 2 });

	document.getElementById("income").innerText = "₹ " + data.totalIncome.toLocaleString("en-IN", { minimumFractionDigits: 2 });
	document.getElementById("expense").innerText = "₹ " + data.totalExpense.toLocaleString("en-IN", { minimumFractionDigits: 2 });

}

// Show selected section only
function showSection(id) {
	document.querySelectorAll(".section").forEach(sec => sec.style.display = "none");
	document.getElementById(id).style.display = "block";
	if (id === "txTable") loadTransactions();
	if (id === "reminder") loadReminders();
	if (id === "budget") loadBudgets();
	if (id === "savings") loadSavingsGoals();
	// You can trigger budget/savings load here if needed
}

// Transaction table loader (reuse your loadTransactions function)
async function loadTransactions() {
	const token = localStorage.getItem("jwt");
	const res = await fetch("/api/expenses/my", {
		headers: { Authorization: "Bearer " + token }
	});

	const data = await res.json();
	const table = document.getElementById("transactionTable");
	table.innerHTML = "";

	data.sort((a, b) => new Date(b.date) - new Date(a.date)); // Most recent first

	data.forEach(tx => {
		const isExpense = tx.type === "EXPENSE";
		const sign = isExpense ? "-" : "+";
		const colorClass = isExpense ? "text-danger" : "text-success";
		const formattedAmount = tx.amount.toLocaleString("en-IN", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2
		});

		const row = `<tr>
	            <td>${tx.date}</td>
	            <td>${tx.category}</td>
	            <td>${tx.description}</td>
	            <td class="${colorClass} fw-bold">${sign} ₹${formattedAmount}</td>
	        </tr>`;
		table.innerHTML += row;
	});


}

async function filterTransactions() {
	const token = localStorage.getItem("jwt");
	const start = document.getElementById("txStartDate").value;
	const end = document.getElementById("txEndDate").value;

	if (!start || !end) {
		Swal.fire({
			icon: 'warning',
			title: 'Missing Dates',
			text: 'Please select both start and end dates.',
			confirmButtonColor: '#ffc107'
		});
		return;
	}

	const res = await fetch(`/api/expenses/filter?start=${start}&end=${end}`, {
		headers: { Authorization: "Bearer " + token }
	});

	const data = await res.json();
	const table = document.getElementById("transactionTable");
	table.innerHTML = "";

	data.sort((a, b) => new Date(b.date) - new Date(a.date)); // Optional but recommended

	data.forEach(tx => {
		const isExpense = tx.type === "EXPENSE";
		const sign = isExpense ? "-" : "+";
		const colorClass = isExpense ? "text-danger" : "text-success";
		const formattedAmount = tx.amount.toLocaleString("en-IN", {
			minimumFractionDigits: 2,
			maximumFractionDigits: 2
		});

		const row = `<tr>
	            <td>${tx.date}</td>
	            <td>${tx.category}</td>
	            <td>${tx.description}</td>
	            <td class="${colorClass} fw-bold">${sign} ₹${formattedAmount}</td>
	        </tr>`;
		table.innerHTML += row;
	});

}

async function resetTransactionFilter() {
	document.getElementById("txStartDate").value = "";
	document.getElementById("txEndDate").value = "";
	loadTransactions();
}

// PDF Download
async function downloadAsPDF() {
	const { jsPDF } = window.jspdf;
	const doc = new jsPDF();

	const logo = new Image();
	logo.src = "/images/VisionWallet.png"; // ✅ Correct image path
	logo.onload = () => {
		// ✅ Add logo (increase height, preserve width aspect ratio)
		doc.addImage(logo, "PNG", 160, 10, 25, 18); // x, y, width, height
		generatePDF();
	};

	function generatePDF() {
		// ✅ User name & date
		const userName = document.getElementById("userName")?.innerText || "User";

		doc.setFontSize(14);
		doc.text(`Hello ${userName},`, 14, 20);
		doc.setFontSize(12);
		doc.text(`Here is your transaction history`, 14, 28);

		// ✅ Get table data
		const table = document.querySelector(".table");
		const headers = [...table.querySelectorAll("thead th")].map(th => th.innerText);

		const rows = [...table.querySelectorAll("tbody tr")]
			.filter(row => row.offsetParent !== null)
			.map(tr => {
				const tds = tr.querySelectorAll("td");
				const date = tds[0].innerText;
				const category = tds[1].innerText;
				const description = tds[2].innerText;
				let amountText = tds[3].innerText.trim();

				// ✅ Remove ₹ and keep +/- only
				amountText = amountText.replace(/₹|\s/g, '');

				const isExpense = amountText.startsWith('-');
				const color = isExpense ? "red" : "green";

				return {
					rowData: [date, category, description, amountText],
					color
				};
			});

		// ✅ Draw autoTable with colored amounts
		doc.autoTable({
			head: [headers],
			body: rows.map(r => r.rowData),
			startY: 36,
			styles: {
				halign: 'left',
				valign: 'middle',
				fontSize: 11,
			},
			headStyles: { fillColor: [137, 138, 196], textColor: 255 },
			didParseCell: function(data) {
				if (data.section === 'body' && data.column.index === 3) {
					const rowColor = rows[data.row.index].color;
					if (rowColor === "green") {
						data.cell.styles.textColor = [34, 139, 34]; // Income
					} else if (rowColor === "red") {
						data.cell.styles.textColor = [200, 0, 0];   // Expense
					}
				}
			}
		});

		doc.save("transaction-history.pdf");
	}
}



// Excel Download
function downloadAsExcel() {
	const table = document.querySelector(".table");
	const wb = XLSX.utils.book_new();

	const headers = [...table.querySelectorAll("thead th")].map(th => th.innerText);

	// 👇 Select only visible rows
	const rows = [...table.querySelectorAll("tbody tr")].filter(row => {
		return row.offsetParent !== null; // visible row
	}).map(tr =>
		[...tr.querySelectorAll("td")].map(td => td.innerText)
	);

	const worksheetData = [headers, ...rows];
	const ws = XLSX.utils.aoa_to_sheet(worksheetData);
	XLSX.utils.book_append_sheet(wb, ws, "Transactions");

	XLSX.writeFile(wb, "transaction-history.xlsx");
}

//Add Transactions
document.getElementById("expenseForm").addEventListener("submit", async function(event) {
	event.preventDefault();

	const amount = parseFloat(document.getElementById("amount").value);
	if (isNaN(amount) || amount < 0) {
		alert("Amount cannot be negative.");
		return;
	}

	const token = localStorage.getItem("jwt");
	const transaction = {
		category: document.getElementById("category").value,
		description: document.getElementById("description").value,
		amount: amount,
		date: document.getElementById("date").value,
		type: document.getElementById("type").value
	};

	const response = await fetch("/api/expenses/add", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
			"Authorization": "Bearer " + token
		},
		body: JSON.stringify(transaction)
	});

	const result = await response.text();

	if (response.ok) {
		// ✅ Show success toast
		const toastElement = document.getElementById('txToast');
		const toast = new bootstrap.Toast(toastElement);
		toast.show();

		loadSummary();     // refresh balance/income/expense
		loadTransactions(); // refresh history
		document.getElementById("expenseForm").reset();
	} else {
		Swal.fire({
			icon: 'error',
			title: 'Transaction Failed',
			text: result || 'Something went wrong!',
			confirmButtonColor: '#dc3545'
		});
	}

});



// Handle reminder form submit
document.getElementById("reminderForm").addEventListener("submit", async function(event) {
	event.preventDefault();

	const amount = parseFloat(document.getElementById("reminderAmount").value);
	if (isNaN(amount) || amount < 0) {
		alert("Amount cannot be negative.");
		return;
	}

	const token = localStorage.getItem("jwt");
	const reminder = {
		title: document.getElementById("reminderTitle").value,
		note: document.getElementById("reminderNote").value,
		dueDate: document.getElementById("reminderDate").value,
		recurring: document.getElementById("reminderRecurring").checked,
		amount: amount
	};

	const url = editingReminderId ? `/api/reminders/${editingReminderId}` : "/api/reminders/add";
	const method = editingReminderId ? "PUT" : "POST";

	const response = await fetch(url, {
		method,
		headers: {
			"Content-Type": "application/json",
			"Authorization": "Bearer " + token
		},
		body: JSON.stringify(reminder)
	});

	const result = await response.text();
	Swal.fire({
		toast: true,
		position: 'top-end',
		icon: 'success',
		title: result,
		showConfirmButton: false,
		timer: 2500,
		timerProgressBar: true
	});


	editingReminderId = null;
	document.getElementById("reminderForm").reset();
	document.querySelector("#reminderForm button[type='submit']").textContent = "Add Reminder";
	document.getElementById("reminderSubmitBtn").textContent = "Add Reminder";

	//It will Reset Button To its original form
	const submitBtn = document.querySelector("#reminderForm button[type='submit']");
	submitBtn.innerHTML = `<i class="fa-solid fa-square-plus"></i> Add Reminder`;
	submitBtn.classList.remove("btn-warning");
	submitBtn.classList.add("btn-primary");

	loadReminders(); // reload reminder table
});



// Load upcoming reminders: reminder’s due date today or within the next 7 days
async function loadReminders() {
	const token = localStorage.getItem("jwt");
	const response = await fetch("/api/reminders/upcoming", {
		headers: {
			"Authorization": "Bearer " + token
		}
	});

	const reminders = await response.json();
	// Sort reminders by due date ascending (oldest to newest)
	reminders.sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));

	const table = document.getElementById("reminderTable");
	table.innerHTML = "";

	const today = new Date().toISOString().split('T')[0]; // yyyy-mm-dd

	reminders.forEach(rem => {
		const row = document.createElement("tr");

		// 🔴 Mark as red if overdue
		if (rem.dueDate < today) {
			row.classList.add("table-danger");
		}

		row.innerHTML = `
	        	  <td>${rem.title}</td>
	        	  <td>${rem.note || "-"}</td>
	        	  <td>${rem.dueDate}</td>
	        	  <td>${rem.recurring ? "Yes" : "No"}</td>
	        	  <td>₹${rem.amount.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
	        	  <td>
	        	    <button class="btn-gradient btn-success" onclick="payReminder(${rem.id})"><i class="fa-solid fa-file-invoice-dollar"></i> Pay</button>
	        	    <button class="btn-gradient btn-warning" onclick='editReminder(${JSON.stringify(rem)})'><i class="fa-solid fa-pen-to-square"></i> Edit</button>
	        	    <button class="btn-gradient btn-danger" onclick="deleteReminder(${rem.id})"><i class="fa-solid fa-trash-can"></i> Delete</button>
	        	  </td>
	        	`;

		table.appendChild(row);
	});
}
//Show Reminder Toast
async function showReminderToasts() {
	const token = localStorage.getItem("jwt");
	const today = new Date().toISOString().split("T")[0];

	try {
		const res = await fetch("/api/reminders/upcoming", {
			headers: {
				Authorization: "Bearer " + token
			}
		});

		const reminders = await res.json();
		const container = document.getElementById("toastContainer");

		reminders.forEach(rem => {
			if (rem.dueDate <= today) {
				const toastEl = document.createElement("div");
				toastEl.className = "toast align-items-center text-bg-danger border-0 mb-2";
				toastEl.setAttribute("role", "alert");
				toastEl.setAttribute("aria-live", "assertive");
				toastEl.setAttribute("aria-atomic", "true");
				toastEl.innerHTML = `
	          <div class="d-flex">
	            <div class="toast-body">
	              🔔 <strong>${rem.title}</strong><br>
	              ${rem.note || "You have a pending reminder due today!"}
	            </div>
	            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
	          </div>
	        `;

				container.appendChild(toastEl);

				const toast = new bootstrap.Toast(toastEl, { delay: 5000 });
				toast.show();
			}
		});
	} catch (error) {
		console.error("Error showing reminders:", error);
	}
}




//payReminder()
async function payReminder(id) {
	const token = localStorage.getItem("jwt");

	// SweetAlert2 Confirmation
	const result = await Swal.fire({
		title: "Pay Reminder?",
		text: "Are you sure you want to mark this reminder as paid?",
		icon: "question",
		showCancelButton: true,
		confirmButtonText: "Yes, Pay Now 💸",
		cancelButtonText: "Cancel"
	});

	if (!result.isConfirmed) return;

	// Proceed with payment
	const res = await fetch(`/api/reminders/pay/${id}`, {
		method: "PUT",
		headers: { Authorization: "Bearer " + token }
	});

	const message = await res.text();

	// ✅ Show success toast after payment
	Swal.fire({
		toast: true,
		position: 'top-end',
		icon: 'success',
		title: message || 'Reminder Paid!',
		showConfirmButton: false,
		timer: 2500,
		timerProgressBar: true
	});

	loadSummary();
	loadTransactions();
	loadReminders();
}



async function deleteReminder(id) {
	const token = localStorage.getItem("jwt");

	const result = await Swal.fire({
		title: "Are you sure?",
		text: "This reminder will be deleted permanently!",
		icon: "warning",
		showCancelButton: true,
		confirmButtonColor: "#d33",
		cancelButtonColor: "#3085d6",
		confirmButtonText: "Yes, delete it!",
		cancelButtonText: "Cancel"
	});

	if (result.isConfirmed) {
		const res = await fetch(`/api/reminders/delete/${id}`, {
			method: "DELETE",
			headers: { Authorization: "Bearer " + token }
		});

		const msg = await res.text();

		Swal.fire({
			icon: "success",
			title: "Deleted!",
			text: msg,
			timer: 2000,
			showConfirmButton: false
		});

		loadReminders();
	}
}


let editingReminderId = null;
function editReminder(rem) {

	document.getElementById("reminderTitle").value = rem.title;
	document.getElementById("reminderNote").value = rem.note;
	document.getElementById("reminderDate").value = rem.dueDate;
	document.getElementById("reminderRecurring").checked = rem.recurring;
	document.getElementById("reminderAmount").value = rem.amount;
	editingReminderId = rem.id;

	// Update button style & icon
	const submitBtn = document.querySelector("#reminderForm button[type='submit']");
	submitBtn.innerHTML = `<i class="fa-solid fa-pen-to-square me-1"></i> Update Reminder`;
	// Replace classes
	submitBtn.classList.remove("btn-success", "btn-info", "btn-primary");
	submitBtn.classList.add("btn-warning", "btn-gradient");
	// Show toast
	Swal.fire({
		toast: true,
		position: 'top-end',
		icon: 'info',
		title: 'Editing reminder...',
		showConfirmButton: false,
		timer: 2000,
		timerProgressBar: true,
		background: '#17a2b8',
		color: '#fff'
	});
}


// 📤 Load budgets for selected month/year
function getMonthName(monthNumber) {
	const monthNames = [
		"January", "February", "March", "April", "May", "June",
		"July", "August", "September", "October", "November", "December"
	];
	return monthNames[monthNumber - 1] || "Invalid";
}

async function loadBudgets(month = "", year = "") {
	const token = localStorage.getItem("jwt");

	let url = "/api/budget/my";
	if (month || year) {
		url += "?";
		if (month) url += `month=${month}&`;
		if (year) url += `year=${year}`;
	}

	const res = await fetch(url, {
		headers: { Authorization: "Bearer " + token }
	});

	const data = await res.json();

	// Sort by year desc, then month desc
	data.sort((a, b) => {
		if (b.year !== a.year) return b.year - a.year;
		return b.month - a.month;
	});

	const table = document.getElementById("budgetTable");
	table.innerHTML = "";

	if (data.length === 0) {
		table.innerHTML = `<tr><td colspan="5">No budgets found</td></tr>`;
		return;
	}

	data.forEach(item => {
		const row = document.createElement("tr");
		row.innerHTML = `
                <td>${item.category}</td>
                <td>₹${item.limitAmount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</td>
                <td>${getMonthName(item.month)}</td>
                <td>${item.year}</td>
                <td>
                    <button class="btn-gradient btn-warning" onclick='editBudget(${JSON.stringify(item)})'><i class="fa-solid fa-pen-to-square"></i> Edit</button>
                    <button class="btn-gradient btn-danger" onclick='deleteBudget(${item.id})'><i class="fa-solid fa-trash-can"></i> Delete</button>
                </td>
            `;
		table.appendChild(row);
	});
}

function applyBudgetFilter() {
	const month = document.getElementById("budgetFilterMonth").value;
	const year = document.getElementById("budgetFilterYear").value;

	// Pass selected values to loadBudgets
	loadBudgets(month, year);
}


function resetBudgetFilter() {
	document.getElementById("budgetFilterMonth").value = "";
	document.getElementById("budgetFilterYear").value = "";
	loadBudgets(); // load all
}


let editingBudgetId = null;

document.getElementById("budgetForm").addEventListener("submit", async function(event) {
	event.preventDefault();

	const token = localStorage.getItem("jwt");
	const category = document.getElementById("budgetCategory").value;
	const limitAmount = parseFloat(document.getElementById("budgetAmount").value);

	if (isNaN(limitAmount) || limitAmount < 0) {
		// Optional: SweetAlert for better UX
		Swal.fire({
			icon: "error",
			title: "Invalid Amount",
			text: "Budget limit cannot be negative.",
		});
		return;
	}

	const month = parseInt(document.getElementById("budgetMonth").value);
	const year = parseInt(document.getElementById("budgetYear").value);
	const payload = { category, limitAmount, month, year };

	const url = editingBudgetId ? `/api/budget/${editingBudgetId}` : "/api/budget/add";
	const method = editingBudgetId ? "PUT" : "POST";

	const res = await fetch(url, {
		method,
		headers: {
			"Content-Type": "application/json",
			"Authorization": "Bearer " + token
		},
		body: JSON.stringify(payload)
	});

	const msg = await res.text();

	if (res.ok) {
		// Show success toast
		const toastElement = document.getElementById("budgetToast");
		toastElement.querySelector(".toast-body").textContent =
			editingBudgetId ? "Budget updated successfully! ✅" : "Budget added successfully!";

		const toast = new bootstrap.Toast(toastElement);
		toast.show();
	} else {
		// Show error alert
		Swal.fire({
			icon: "error",
			title: "Error",
			text: msg,
		});
	}

	editingBudgetId = null;
	document.getElementById("budgetForm").reset();
	const submitBtn = document.querySelector("#budgetForm button[type='submit']");
	submitBtn.innerHTML = '<i class="fa-solid fa-square-plus"></i> Add Budget';
	submitBtn.className = 'btn-gradient btn-primary';


	loadBudgets();
});


// 🧹 Delete budget
async function deleteBudget(id) {
	const token = localStorage.getItem("jwt");

	// Show SweetAlert confirmation
	const result = await Swal.fire({
		title: 'Are you sure?',
		text: 'This budget will be permanently deleted.',
		icon: 'warning',
		showCancelButton: true,
		confirmButtonText: 'Yes, delete it!',
		cancelButtonText: 'Cancel',
		confirmButtonColor: '#d33',
		cancelButtonColor: '#3085d6'
	});

	if (!result.isConfirmed) return;

	// Proceed with delete if confirmed
	const res = await fetch(`/api/budget/${id}`, {
		method: "DELETE",
		headers: { Authorization: "Bearer " + token }
	});

	const msg = await res.text();

	// Show SweetAlert success after deletion
	Swal.fire({
		icon: 'success',
		title: 'Deleted!',
		text: msg,
		timer: 2000,
		showConfirmButton: false
	});

	loadBudgets(); // Refresh the table
}


// ✏️ Edit budget
function editBudget(budget) {
	document.getElementById("budgetCategory").value = budget.category;
	document.getElementById("budgetAmount").value = budget.limitAmount;
	document.getElementById("budgetMonth").value = budget.month;
	document.getElementById("budgetYear").value = budget.year;

	editingBudgetId = budget.id;
	const submitBtn = document.querySelector("#budgetForm button[type='submit']");
	submitBtn.innerHTML = '<i class="fa-solid fa-pen-to-square"></i> Update Budget';
	submitBtn.className = "btn-gradient btn-warning";

}
function editBudgetFromRow(row) {
	document.getElementById("budgetCategory").value = row.dataset.category;
	document.getElementById("budgetAmount").value = row.dataset.amount;
	document.getElementById("budgetMonth").value = row.dataset.month;
	document.getElementById("budgetYear").value = row.dataset.year;

	editingBudgetId = row.dataset.id;

	const submitBtn = document.querySelector("#budgetForm button[type='submit']");
	submitBtn.innerHTML = '<i class="fa-solid fa-pen-to-square"></i> Update Budget';
	submitBtn.className = "btn-gradient btn-warning";

}




// Submit new goal with validation and toast
document.getElementById("savingsForm").addEventListener("submit", async function(event) {
	event.preventDefault();

	const targetAmount = parseFloat(document.getElementById("targetAmount").value);
	if (isNaN(targetAmount) || targetAmount < 0) {
		Swal.fire({
			icon: "warning",
			title: "Invalid Amount",
			text: "Target amount cannot be negative."
		});
		return;
	}

	const goal = {
		goalName: document.getElementById("goalName").value,
		description: document.getElementById("goalDescription").value,
		targetAmount: targetAmount
	};

	const token = localStorage.getItem("jwt");

	const res = await fetch("/api/savings/add", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
			"Authorization": "Bearer " + token
		},
		body: JSON.stringify(goal)
	});

	const msg = await res.text();

	if (res.ok) {
		document.getElementById("savingsForm").reset();
		loadSavingsGoals();

		// ✅ Show success toast
		const toastEl = document.getElementById("goalAddToast");
		toastEl.querySelector(".toast-body").textContent = msg;
		const toast = new bootstrap.Toast(toastEl);
		toast.show();
	} else {
		Swal.fire({
			icon: "error",
			title: "Failed",
			text: msg
		});
	}
});


//Load goals and render table
async function loadSavingsGoals() {
	const token = localStorage.getItem("jwt");
	const res = await fetch("/api/savings/my", {
		headers: { Authorization: "Bearer " + token }
	});

	const goals = await res.json();

	// 🔽 Sort: Incomplete goals first, then completed and purchased
	goals.sort((a, b) => {
		const order = { PENDING: 0, COMPLETED: 1, PURCHASED: 2 };
		return order[a.status] - order[b.status];
	});

	const table = document.getElementById("savingsTable");
	table.innerHTML = "";

	goals.forEach(goal => {
		const targetFormatted = goal.targetAmount.toLocaleString("en-IN", {
			minimumFractionDigits: 2, maximumFractionDigits: 2
		});

		const savedFormatted = goal.savedAmount.toLocaleString("en-IN", {
			minimumFractionDigits: 2, maximumFractionDigits: 2
		});

		let actions = "";

		if (goal.status === "COMPLETED") {
			actions = `
	                <button class="btn-gradient btn-primary" onclick="buyGoal(${goal.id})"><i class="fa-solid fa-bag-shopping"></i> Buy</button>
	                <button class="btn-gradient btn-secondary" onclick="withdrawGoal(${goal.id})"><i class="fa-solid fa-money-bill-transfer"></i> Withdraw</button>
	            `;
		} else if (goal.status === "PURCHASED") {
			actions = `<span class="text-success">Purchased</span>`;
		} else {
			actions = `
	                <button class="btn-gradient btn-success" onclick="updateProgress(${goal.id})"><i class="fa-solid fa-piggy-bank"></i> Add</button>
	                <button class="btn-gradient btn-warning ms-1" onclick="withdrawFromGoal(${goal.id})"><i class="fa-solid fa-money-bill-transfer"></i> Withdraw</button>
	                <button class="btn-gradient btn-danger ms-1" onclick="deleteGoal(${goal.id})"><i class="fa-solid fa-trash-can"></i> Delete</button>
	            `;
		}

		const row = document.createElement("tr");
		row.innerHTML = `
	            <td>${goal.goalName}</td>
	            <td>${goal.description || "-"}</td>
	            <td>₹${targetFormatted}</td>
	            <td>₹${savedFormatted}</td>
	            <td>${goal.status}</td>
	            <td>${actions}</td>
	        `;

		table.appendChild(row);
	});
}


// Withdraw Money From Goal (SweetAlert2 only)
async function withdrawFromGoal(goalId) {
	const { value: amount } = await Swal.fire({
		title: "Withdraw Amount",
		input: "number",
		inputLabel: "Enter amount to withdraw:",
		inputPlaceholder: "e.g. 500",
		inputAttributes: {
			min: 0.1,
			step: 0.01
		},
		showCancelButton: true,
		confirmButtonText: "Withdraw",
		cancelButtonText: "Cancel",
		inputValidator: (value) => {
			if (!value || isNaN(value) || parseFloat(value) <= 0) {
				return "Please enter a valid positive number.";
			}
		}
	});

	if (!amount) return; // user cancelled

	const token = localStorage.getItem("jwt");

	try {
		const res = await fetch(`/api/savings/withdraw/${goalId}?amount=${amount}`, {
			method: "PUT",
			headers: { Authorization: "Bearer " + token }
		});

		const msg = await res.text();

		if (!res.ok || msg.toLowerCase().includes("insufficient") || msg.toLowerCase().includes("exceed")) {
			// ❌ Handle backend failure message
			Swal.fire({
				icon: "error",
				title: "Withdrawal Failed",
				text: msg,
				confirmButtonColor: "#d33"
			});
		} else {
			// ✅ Success
			Swal.fire({
				icon: "success",
				title: "Withdrawal Successful",
				text: msg,
				timer: 2500,
				showConfirmButton: false
			});

			loadSavingsGoals();
			loadSummary();
		}
	} catch (error) {
		Swal.fire({
			icon: "error",
			title: "Error",
			text: "Something went wrong while processing your request."
		});
	}
}




// ✅ Update saved amount using SweetAlert2
async function updateProgress(goalId) {
	const { value: amountInput } = await Swal.fire({
		title: "Add to Savings",
		input: "number",
		inputLabel: "Enter amount to add to your savings goal:",
		inputPlaceholder: "e.g. 1000",
		inputAttributes: {
			min: 0.1,
			step: 0.01
		},
		showCancelButton: true,
		confirmButtonText: "Add",
		cancelButtonText: "Cancel",
		inputValidator: (value) => {
			if (!value || isNaN(value) || parseFloat(value) <= 0) {
				return "Please enter a valid positive number.";
			}
		}
	});

	if (amountInput) {
		const amountToAdd = parseFloat(amountInput);
		const token = localStorage.getItem("jwt");

		try {
			const res = await fetch(`/api/savings/update/${goalId}?amountToAdd=${amountToAdd}`, {
				method: "PUT",
				headers: {
					Authorization: "Bearer " + token
				}
			});

			const msg = await res.text();

			Swal.fire({
				icon: "success",
				title: "Saved Successfully!",
				text: msg,
				timer: 2500,
				showConfirmButton: false
			});

			loadSummary();
			loadSavingsGoals();
		} catch (error) {
			console.error("Failed to update goal progress", error);
			Swal.fire({
				icon: "error",
				title: "Error",
				text: "Something went wrong while updating the savings goal."
			});
		}
	}
}


// ❌ Delete The Goal with confirmation using SweetAlert2
async function deleteGoal(id) {
	const result = await Swal.fire({
		title: "Delete Savings Goal?",
		text: "Are you sure you want to delete this savings goal? Its saved amount will be added back to your balance.",
		icon: "warning",
		showCancelButton: true,
		confirmButtonColor: "#d33",
		cancelButtonColor: "#3085d6",
		confirmButtonText: "Yes, delete it!",
		cancelButtonText: "Cancel"
	});

	if (result.isConfirmed) {
		const token = localStorage.getItem("jwt");

		const res = await fetch(`/api/savings/delete/${id}`, {
			method: "DELETE",
			headers: {
				Authorization: "Bearer " + token
			}
		});

		const msg = await res.text();

		await Swal.fire({
			icon: "success",
			title: "Deleted!",
			text: msg,
			timer: 2500,
			showConfirmButton: false
		});

		loadSavingsGoals();
		loadSummary(); // 💰 Update balance
	}
}


// 🛍️ Buy Handler with SweetAlert2 Toast
async function buyGoal(goalId) {
	const token = localStorage.getItem("jwt");

	const res = await fetch(`/api/savings/buy/${goalId}`, {
		method: "PUT",
		headers: { Authorization: "Bearer " + token }
	});

	const msg = await res.text();

	// ✅ Show SweetAlert2 Toast
	Swal.fire({
		toast: true,
		position: 'top-end',
		icon: 'success',
		title: msg,
		showConfirmButton: false,
		timer: 3000,
		timerProgressBar: true
	});

	loadSavingsGoals();
	loadSummary();
	loadTransactions();
}


//Withdraw Handler with confirmation and toast
async function withdrawGoal(goalId) {
	const token = localStorage.getItem("jwt");

	// Optional: SweetAlert confirmation
	const result = await Swal.fire({
		title: "Are you sure?",
		text: "Withdraw saved amount from this goal?",
		icon: "warning",
		showCancelButton: true,
		confirmButtonColor: "#dc3545",
		cancelButtonColor: "#6c757d",
		confirmButtonText: "Yes, withdraw"
	});

	if (!result.isConfirmed) return;

	const res = await fetch(`/api/savings/withdraw/${goalId}`, {
		method: "DELETE",
		headers: { Authorization: "Bearer " + token }
	});

	const msg = await res.text();

	// ✅ Show toast
	const toastElement = document.getElementById("goalToast");
	toastElement.querySelector(".toast-body").textContent = msg;
	const toast = new bootstrap.Toast(toastElement);
	toast.show();

	loadSavingsGoals();
	loadSummary();
}




function logout() {
	Swal.fire({
		title: 'Are you sure?',
		text: "You will be logged out of VisionWallet.",
		icon: 'warning',
		showCancelButton: true,
		confirmButtonColor: '#d33',
		cancelButtonColor: '#3085d6',
		confirmButtonText: 'Yes, logout',
		cancelButtonText: 'Cancel'
	}).then((result) => {
		if (result.isConfirmed) {
			localStorage.removeItem("jwt");
			window.location.href = "/";
		}
	});
}


loadSummary(); // Load summary on page load

// Show "View Transactions" section by default on load
document.addEventListener("DOMContentLoaded", function() {
	// ✅ Show Transactions section by default
	showSection("txTable");

	// ✅ Check if reminders should be shown
	const shouldShowReminders = localStorage.getItem("showReminders");
	if (shouldShowReminders === "true") {
		localStorage.removeItem("showReminders");
		showReminderToasts(); // ✅ show toast reminders once
	}
});
