const fs = require('fs');
const path = 'C:\\Users\\Asus\\.gemini\\antigravity\\brain\\79842893-f528-4756-8b58-9dd10cc19345\\.system_generated\\logs\\transcript.jsonl';
const lines = fs.readFileSync(path, 'utf-8').split('\n');

for (let i = 0; i < Math.min(lines.length, 100); i++) {
    const line = lines[i];
    if (!line) continue;
    try {
        const data = JSON.parse(line);
        if (JSON.stringify(data).includes('PatientPortalDialog.kt')) {
            console.log(`Step ${data.step_index || i}: type=${data.type}, source=${data.source}, status=${data.status}`);
            if (data.tool_calls) {
                for (const tc of data.tool_calls) {
                    console.log(`  Tool Call: ${tc.name}`);
                }
            }
        }
    } catch (e) {}
}
