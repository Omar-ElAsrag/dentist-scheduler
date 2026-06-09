const fs = require('fs');
const path = 'C:\\Users\\Asus\\.gemini\\antigravity\\brain\\79842893-f528-4756-8b58-9dd10cc19345\\.system_generated\\logs\\transcript.jsonl';
const lines = fs.readFileSync(path, 'utf-8').split('\n');

for (const line of lines) {
    if (!line) continue;
    try {
        const data = JSON.parse(line);
        if (data.step_index === 56 || data.step_index === 57) {
            console.log(`Step ${data.step_index}: type=${data.type}, source=${data.source}, status=${data.status}`);
            console.log('Keys:', Object.keys(data));
            if (data.tool_calls) {
                console.log('Tool calls:', JSON.stringify(data.tool_calls, null, 2));
            }
            if (data.content) {
                console.log('Content length:', data.content.length);
                console.log('Content preview:', data.content.substring(0, 500));
            }
        }
    } catch (e) {}
}
