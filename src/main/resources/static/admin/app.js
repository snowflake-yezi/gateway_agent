const keyInput = document.querySelector('#api-key');
const pageStatus = document.querySelector('#page-status');
const modelSelect = document.querySelector('#model-select');

// Keep the admin page dependency-free; localStorage is only for local browser convenience.
keyInput.value = localStorage.getItem('gateway.apiKey') || '';

document.querySelector('#save-key').addEventListener('click', () => {
  localStorage.setItem('gateway.apiKey', keyInput.value.trim());
  setStatus('API key saved');
});

document.querySelector('#clear-key').addEventListener('click', () => {
  localStorage.removeItem('gateway.apiKey');
  keyInput.value = '';
  setStatus('API key cleared');
});

// Health endpoints are public; /v1 endpoints below attach the gateway API key.
document.querySelector('#check-health').addEventListener('click', () => getJson('/health', 'health-output', false));
document.querySelector('#check-actuator').addEventListener('click', () => getJson('/actuator/health', 'health-output', false));
document.querySelector('#load-models').addEventListener('click', loadModels);
document.querySelector('#send-chat').addEventListener('click', sendChat);

async function loadModels() {
  const data = await getJson('/v1/models', 'models-output', true);
  if (!data || !Array.isArray(data.data)) {
    return;
  }
  modelSelect.innerHTML = '';
  data.data.forEach((model) => {
    const option = document.createElement('option');
    option.value = model.id;
    option.textContent = model.id;
    modelSelect.appendChild(option);
  });
}

async function sendChat() {
  const output = document.querySelector('#chat-output');
  const assistantOutput = document.querySelector('#assistant-output');
  const body = {
    model: modelSelect.value || 'gateway-stub-chat',
    messages: [
      {
        role: 'user',
        content: document.querySelector('#message-input').value || 'hello',
      },
    ],
    temperature: Number(document.querySelector('#temperature-input').value || 0.7),
  };

  try {
    const response = await fetch('/v1/chat/completions', {
      method: 'POST',
      headers: authHeaders({'Content-Type': 'application/json'}),
      body: JSON.stringify(body),
    });
    const data = await response.json();
    output.textContent = JSON.stringify(data, null, 2);
    // Error responses do not have choices, so optional chaining keeps the UI useful while debugging.
    assistantOutput.textContent = data?.choices?.[0]?.message?.content || '(no assistant content)';
    setStatus(response.ok ? 'chat ok' : `chat failed ${response.status}`, !response.ok);
  } catch (error) {
    output.textContent = String(error);
    assistantOutput.textContent = 'request failed';
    setStatus('chat failed', true);
  }
}

async function getJson(url, outputId, withAuth) {
  const output = document.querySelector(`#${outputId}`);
  try {
    const response = await fetch(url, { headers: withAuth ? authHeaders() : {} });
    const data = await response.json();
    output.textContent = JSON.stringify(data, null, 2);
    setStatus(response.ok ? `${url} ok` : `${url} failed ${response.status}`, !response.ok);
    return data;
  } catch (error) {
    output.textContent = String(error);
    setStatus(`${url} failed`, true);
    return null;
  }
}

function authHeaders(extra = {}) {
  const apiKey = keyInput.value.trim() || localStorage.getItem('gateway.apiKey') || '';
  return apiKey ? { ...extra, Authorization: `Bearer ${apiKey}` } : extra;
}

function setStatus(text, error = false) {
  pageStatus.textContent = text;
  pageStatus.classList.toggle('error', error);
}
