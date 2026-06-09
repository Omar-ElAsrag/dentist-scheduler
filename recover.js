const fs = require('fs');
const path = 'C:\\Users\\Asus\\.gemini\\antigravity\\brain\\79842893-f528-4756-8b58-9dd10cc19345\\.system_generated\\logs\\transcript.jsonl';
const lines = fs.readFileSync(path, 'utf-8').split('\n');

let diffOutput = '';
for (const line of lines) {
    if (!line) continue;
    try {
        const data = JSON.parse(line);
        if (data.type === 'TOOL_RESPONSE' && data.content && data.content.includes('PatientPortalDialog.kt') && data.content.includes('[diff_block_start]')) {
            diffOutput = data.content;
        }
    } catch (e) {}
}

if (diffOutput) {
    const startIdx = diffOutput.indexOf('@@ -1509,652');
    const endIdx = diffOutput.indexOf('[diff_block_end]', startIdx);
    const diffBlock = diffOutput.substring(startIdx, endIdx);
    
    const recovered = [];
    const dbLines = diffBlock.split('\n');
    for (let i = 1; i < dbLines.length; i++) {
        const dl = dbLines[i];
        if (dl.startsWith('-')) {
            recovered.push(dl.substring(1));
        }
    }
    
    fs.writeFileSync('d:\\dentist-scheduler\\recovery.txt', recovered.join('\n'), 'utf-8');
    console.log('Recovered ' + recovered.length + ' lines');
} else {
    console.log('Diff output not found');
}
