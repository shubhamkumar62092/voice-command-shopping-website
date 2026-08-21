// ---------------------------------------------------------------------
// Voice Command Shopping Assistant — frontend
// Talks to the Spring Boot API at API_BASE (same-origin, since Spring
// Boot now serves this file too). Uses the browser's built-in Web Speech
// API for voice-to-text, then sends the raw transcript to the backend,
// which does the NLP-lite intent parsing (see CommandParser.java).
// ---------------------------------------------------------------------

const API_BASE = "/api";

// Fixed aisle order so the receipt reads the way a store is laid out,
// rather than alphabetically.
const AISLE_ORDER = [
  "Produce", "Dairy", "Bakery", "Meat & Seafood", "Frozen",
  "Pantry", "Snacks", "Beverages", "Household", "General",
];

const micButton = document.getElementById("micButton");
const micStatus = document.getElementById("micStatus");
const ring1 = document.getElementById("ring1");
const ring2 = document.getElementById("ring2");
const waveform = document.getElementById("waveform");
const transcriptLine = document.getElementById("transcriptLine");
const langSelect = document.getElementById("langSelect");
const manualForm = document.getElementById("manualForm");
const manualInput = document.getElementById("manualInput");
const toastStack = document.getElementById("toastStack");
const aisleContainer = document.getElementById("aisleContainer");
const emptyStateEl = document.getElementById("emptyState");
const countBadge = document.getElementById("countBadge");
const clearBtn = document.getElementById("clearBtn");
const statsStrip = document.getElementById("statsStrip");
const statsBarFill = document.getElementById("statsBarFill");
const statsLabel = document.getElementById("statsLabel");
const listSkeleton = document.getElementById("listSkeleton");
const purchasedFold = document.getElementById("purchasedFold");
const purchasedToggle = document.getElementById("purchasedToggle");
const purchasedToggleLabel = document.getElementById("purchasedToggleLabel");
const purchasedList = document.getElementById("purchasedList");
const suggestionsSection = document.getElementById("suggestions");
const suggestionChips = document.getElementById("suggestionChips");
const swapSection = document.getElementById("swapSection");
const swapChips = document.getElementById("swapChips");
const dateStamp = document.getElementById("dateStamp");

let isListening = false;
let recognition = null;
let allActiveItems = [];

// ---------- Header date stamp ----------

(function setDateStamp() {
  const today = new Date();
  const formatted = today.toLocaleDateString(undefined, {
    month: "short", day: "numeric", year: "numeric",
  });
  dateStamp.textContent = formatted + " — Today's List";
})();

// ---------- Speech recognition setup ----------

const SpeechRecognitionImpl = window.SpeechRecognition || window.webkitSpeechRecognition;

function initRecognition() {
  if (!SpeechRecognitionImpl) {
    micStatus.textContent = "Voice not supported — type instead";
    micButton.disabled = true;
    micButton.style.opacity = 0.4;
    return;
  }

  recognition = new SpeechRecognitionImpl();
  recognition.continuous = false;
  recognition.interimResults = true;
  recognition.maxAlternatives = 1;
  recognition.lang = langSelect.value;

  recognition.onstart = () => {
    isListening = true;
    micButton.classList.add("listening");
    micButton.setAttribute("aria-pressed", "true");
    ring1.classList.add("active");
    ring2.classList.add("active");
    waveform.classList.add("active");
    micStatus.textContent = "Listening…";
    transcriptLine.textContent = "";
  };

  recognition.onresult = (event) => {
    let text = "";
    for (let i = 0; i < event.results.length; i++) {
      text += event.results[i][0].transcript;
    }
    transcriptLine.textContent = text;

    const last = event.results[event.results.length - 1];
    if (last.isFinal) {
      submitTranscript(text.trim());
    }
  };

  recognition.onerror = (event) => {
    stopListeningUI();
    if (event.error === "no-speech") {
      showToast("Didn't hear anything — try again.", "error");
    } else if (event.error === "not-allowed" || event.error === "service-not-allowed") {
      showToast("Microphone access was blocked. Check your browser permissions.", "error");
    } else {
      showToast("Voice recognition error: " + event.error, "error");
    }
  };

  recognition.onend = () => {
    stopListeningUI();
  };
}

function stopListeningUI() {
  isListening = false;
  micButton.classList.remove("listening");
  micButton.setAttribute("aria-pressed", "false");
  ring1.classList.remove("active");
  ring2.classList.remove("active");
  waveform.classList.remove("active");
  micStatus.textContent = "Tap to speak";
}

micButton.addEventListener("click", () => {
  if (!recognition) return;
  if (isListening) {
    recognition.stop();
    return;
  }
  recognition.lang = langSelect.value;
  try {
    recognition.start();
  } catch (err) {
    // start() throws if called while already running; ignore.
  }
});

langSelect.addEventListener("change", () => {
  if (recognition) recognition.lang = langSelect.value;
});

// ---------- Manual text fallback ----------

manualForm.addEventListener("submit", (e) => {
  e.preventDefault();
  const text = manualInput.value.trim();
  if (!text) return;
  submitTranscript(text);
  manualInput.value = "";
});

// ---------- Clear list ----------

clearBtn.addEventListener("click", async () => {
  if (allActiveItems.length === 0) return;
  const sure = confirm("Clear your entire list? This can't be undone.");
  if (!sure) return;
  try {
    await fetch(`${API_BASE}/items`, { method: "DELETE" });
    showToast("List cleared.", "success");
    await refreshList();
    await refreshSuggestions();
  } catch (err) {
    showToast("Couldn't clear the list.", "error");
  }
});

// ---------- Purchased fold ----------

purchasedToggle.addEventListener("click", () => {
  const expanded = purchasedToggle.getAttribute("aria-expanded") === "true";
  purchasedToggle.setAttribute("aria-expanded", String(!expanded));
  purchasedList.hidden = expanded;
});

// ---------- Toasts ----------

function showToast(message, type = "info", timeout = 4200) {
  const toast = document.createElement("div");
  toast.className = "toast " + type;
  toast.setAttribute("role", type === "error" ? "alert" : "status");

  const icon = document.createElement("span");
  icon.className = "toast-icon";
  icon.textContent = type === "success" ? "✓" : type === "error" ? "×" : "•";

  const text = document.createElement("span");
  text.textContent = message;

  toast.appendChild(icon);
  toast.appendChild(text);
  toastStack.appendChild(toast);

  setTimeout(() => {
    toast.classList.add("leaving");
    setTimeout(() => toast.remove(), 220);
  }, timeout);
}

// ---------- API calls ----------

async function submitTranscript(transcript) {
  if (!transcript) return;
  swapSection.hidden = true;

  try {
    const res = await fetch(`${API_BASE}/voice-command`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ transcript }),
    });

    if (!res.ok) throw new Error("Server responded with " + res.status);
    const result = await res.json();

    showToast(result.message, result.success ? "success" : "error");

    if (result.intent === "SEARCH" && Array.isArray(result.searchResults)) {
      renderAisles(result.searchResults, { isSearchView: true });
    } else {
      await refreshList();
    }

    if (result.substituteSuggestions && result.substituteSuggestions.length) {
      renderSwapSuggestions(result.affectedItem, result.substituteSuggestions);
    }

    await refreshSuggestions();
  } catch (err) {
    showToast(
      "Couldn't reach the server. Is Spring Boot still running?",
      "error",
      6000
    );
    console.error(err);
  }
}

async function refreshList() {
  try {
    const res = await fetch(`${API_BASE}/items`);
    if (!res.ok) throw new Error("Failed to load list");
    const items = await res.json();
    allActiveItems = items;
    listSkeleton.hidden = true;
    renderAisles(items, { isSearchView: false });
  } catch (err) {
    listSkeleton.hidden = true;
    console.error(err);
  }
}

async function refreshSuggestions() {
  try {
    const res = await fetch(`${API_BASE}/suggestions`);
    if (!res.ok) return;
    const data = await res.json();
    renderSuggestions(data);
  } catch (err) {
    console.error(err);
  }
}

async function toggleItemPurchased(id) {
  try {
    await fetch(`${API_BASE}/items/${id}/purchased`, { method: "PATCH" });
    await refreshList();
  } catch (err) {
    showToast("Couldn't update that item.", "error");
  }
}

async function deleteItem(id) {
  try {
    await fetch(`${API_BASE}/items/${id}`, { method: "DELETE" });
    await refreshList();
    await refreshSuggestions();
  } catch (err) {
    showToast("Couldn't remove that item.", "error");
  }
}

// ---------- Rendering: aisle-grouped list ----------

function buildItemRow(item, { showRemove }) {
  const li = document.createElement("li");
  li.className = "item-row" + (item.purchased ? " purchased" : "");

  const check = document.createElement("button");
  check.className = "item-check" + (item.purchased ? " checked" : "");
  check.type = "button";
  check.setAttribute("aria-label", item.purchased ? "Mark not purchased" : "Mark purchased");
  check.innerHTML = item.purchased ? "✓" : "";
  check.addEventListener("click", () => toggleItemPurchased(item.id));

  const main = document.createElement("div");
  main.className = "item-main";

  const name = document.createElement("span");
  name.className = "item-name";
  name.textContent = item.name;

  const leader = document.createElement("span");
  leader.className = "item-leader";

  const qty = document.createElement("span");
  qty.className = "item-qty";
  qty.textContent = item.quantity + (item.unit ? " " + item.unit : "x");

  main.appendChild(name);
  main.appendChild(leader);
  main.appendChild(qty);

  const tag = document.createElement("span");
  tag.className = "item-tag";
  tag.textContent = item.category || "General";

  li.appendChild(check);
  li.appendChild(main);
  li.appendChild(tag);

  if (showRemove) {
    const remove = document.createElement("button");
    remove.className = "item-remove";
    remove.type = "button";
    remove.setAttribute("aria-label", "Remove " + item.name);
    remove.textContent = "×";
    remove.addEventListener("click", () => deleteItem(item.id));
    li.appendChild(remove);
  }

  return li;
}

function renderAisles(items, { isSearchView }) {
  aisleContainer.innerHTML = "";
  purchasedFold.hidden = true;
  purchasedList.innerHTML = "";

  const active = isSearchView ? items : items.filter((i) => !i.purchased);
  const purchased = isSearchView ? [] : items.filter((i) => i.purchased);

  updateStats(active, purchased);

  if (active.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.innerHTML = isSearchView
      ? "<p>No matches found.</p>"
      : `<svg viewBox="0 0 48 48" width="34" height="34" fill="none" stroke="currentColor" stroke-width="1.5">
           <path d="M8 14h32l-3 22a3 3 0 0 1-3 3H14a3 3 0 0 1-3-3L8 14Z"/>
           <path d="M16 14V10a8 8 0 0 1 16 0v4" stroke-linecap="round"/>
         </svg>
         <p>Your list is empty.<br>Try saying <span class="voice-eg">“add milk.”</span></p>`;
    aisleContainer.appendChild(empty);
  } else {
    const grouped = groupByAisle(active);
    grouped.forEach(([category, groupItems]) => {
      const group = document.createElement("div");
      group.className = "aisle-group";

      const head = document.createElement("div");
      head.className = "aisle-head";
      head.innerHTML = `<span class="aisle-name">${escapeHtml(category)}</span>
                         <span class="aisle-rule"></span>
                         <span class="aisle-count">${groupItems.length}</span>`;
      group.appendChild(head);

      const ul = document.createElement("ul");
      ul.className = "list";
      groupItems.forEach((item) => ul.appendChild(buildItemRow(item, { showRemove: !isSearchView })));
      group.appendChild(ul);

      aisleContainer.appendChild(group);
    });
  }

  countBadge.textContent = active.length + (active.length === 1 ? " item" : " items");

  if (!isSearchView && purchased.length > 0) {
    purchasedFold.hidden = false;
    purchasedToggleLabel.textContent = `Picked up (${purchased.length})`;
    purchased.forEach((item) => purchasedList.appendChild(buildItemRow(item, { showRemove: true })));
  }
}

function groupByAisle(items) {
  const map = new Map();
  items.forEach((item) => {
    const cat = item.category || "General";
    if (!map.has(cat)) map.set(cat, []);
    map.get(cat).push(item);
  });
  return Array.from(map.entries()).sort((a, b) => {
    const ai = AISLE_ORDER.indexOf(a[0]);
    const bi = AISLE_ORDER.indexOf(b[0]);
    return (ai === -1 ? 999 : ai) - (bi === -1 ? 999 : bi);
  });
}

function updateStats(active, purchased) {
  const total = active.length + purchased.length;
  if (total === 0) {
    statsStrip.hidden = true;
    return;
  }
  statsStrip.hidden = false;
  const pct = total === 0 ? 0 : Math.round((purchased.length / total) * 100);
  statsBarFill.style.width = pct + "%";
  statsLabel.textContent = `${purchased.length} of ${total} picked up`;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

// ---------- Rendering: suggestions & swaps ----------

function renderSuggestions(data) {
  const chips = [];

  (data.frequentlyBought || []).forEach((name) => {
    chips.push({ name, className: "", label: name });
  });
  (data.seasonal || []).forEach((name) => {
    chips.push({ name, className: "seasonal", label: name + " 🌱" });
  });

  if (chips.length === 0) {
    suggestionsSection.hidden = true;
    return;
  }

  suggestionsSection.hidden = false;
  suggestionChips.innerHTML = "";

  chips.forEach(({ name, className, label }) => {
    const chip = document.createElement("button");
    chip.className = "chip" + (className ? " " + className : "");
    chip.type = "button";
    chip.textContent = label;
    chip.addEventListener("click", () => submitTranscript("add " + name));
    suggestionChips.appendChild(chip);
  });
}

function renderSwapSuggestions(affectedItem, substitutes) {
  if (!affectedItem || !substitutes || substitutes.length === 0) {
    swapSection.hidden = true;
    return;
  }
  swapSection.hidden = false;
  swapChips.innerHTML = "";

  substitutes.forEach((sub) => {
    const chip = document.createElement("button");
    chip.className = "chip swap";
    chip.type = "button";
    chip.textContent = "Swap for " + sub;
    chip.addEventListener("click", async () => {
      await submitTranscript("remove " + affectedItem.name);
      await submitTranscript("add " + sub);
      swapSection.hidden = true;
    });
    swapChips.appendChild(chip);
  });
}

// ---------- Init ----------

initRecognition();
listSkeleton.hidden = false;
refreshList();
refreshSuggestions();
