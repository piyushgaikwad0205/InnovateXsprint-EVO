import express from 'express';

const app = express();
app.use(express.json());

const MODEL = 'grok-4-latest';
const XAI_URL = 'https://api.x.ai/v1/chat/completions';

app.post('/generate-study-plan', async (req, res) => {
  const apiKey = process.env.XAI_API_KEY;
  if (!apiKey) return res.status(500).json({ error: 'missing_api_key' });

  const { goal, dailyMinutes, days, preset } = req.body || {};

  const system = `You generate study plans. Reply ONLY with JSON: {"days":[{"date":"YYYY-MM-DD","sessions":[{"time":"HH:mm","title":"...","durationMinutes":45,"focus":"High","notes":"..."}]}],"tips":["..."]}`;

  const user = `Goal: ${goal || 'General upskill'}\nDaily minutes: ${dailyMinutes || 90}\nDays: ${days || 5}\nPreset: ${preset || 'standard'}\nKeep 2-4 sessions/day, include short breaks in notes.`;

  const body = {
    model: MODEL,
    temperature: 0.2,
    messages: [
      { role: 'system', content: system },
      { role: 'user', content: user }
    ]
  };

  try {
    const resp = await fetch(XAI_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`
      },
      body: JSON.stringify(body)
    });

    if (!resp.ok) {
      const detail = await resp.text();
      return res.status(500).json({ error: 'xai_error', detail });
    }

    const data = await resp.json();
    const content = data?.choices?.[0]?.message?.content;
    if (!content) return res.status(500).json({ error: 'empty_response' });

    let parsed;
    try {
      parsed = JSON.parse(content);
    } catch (e) {
      return res.status(500).json({ error: 'json_parse_failed', raw: content });
    }

    return res.json(parsed);
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'server_error' });
  }
});

const PORT = process.env.PORT || 3003;
app.listen(PORT, () => console.log(`AI backend running on :${PORT}`));
