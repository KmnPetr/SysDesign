const CHART_HEIGHT = 250; // k6 web dashboard uPlot height

export function buildChartsInjection(samples) {
    if (!samples || samples.length === 0) {
        return '';
    }

    const start = samples[0].timestamp;
    const labels = samples.map((s) => `${Math.round((s.timestamp - start) / 1000)}s`);
    const cpu = samples.map((s) => s.cpu);
    const ram = samples.map((s) => s.ram ?? s.ramCurrent);
    const ramMax = samples.map((s) => s.ramMax);
    const disk = samples.map((s) => s.disk ?? s.diskSpeed);

    return `
<!-- k6-server-metrics -->
<section id="server-metrics-charts" style="margin:32px 24px 48px;font-family:sans-serif;color:#eee;">
    <h2 style="margin:0 0 8px;font-size:22px;">Метрики сервера (node-exporter)</h2>
    <p style="margin:0 0 20px;color:#aaa;">Точек: ${samples.length}, опрос 1 раз/с</p>
    <div style="margin-bottom:16px;height:${CHART_HEIGHT}px;">
        <canvas id="k6ServerCpuChart"></canvas>
    </div>
    <div style="margin-bottom:16px;height:${CHART_HEIGHT}px;">
        <canvas id="k6ServerRamChart"></canvas>
    </div>
    <div style="margin-bottom:16px;height:${CHART_HEIGHT}px;">
        <canvas id="k6ServerDiskChart"></canvas>
    </div>
</section>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script>
(() => {
    const labels = ${JSON.stringify(labels)};
    const commonOptions = {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        scales: {
            x: { ticks: { color: '#aaa' }, grid: { color: '#333' } },
            y: { ticks: { color: '#aaa' }, grid: { color: '#333' } },
        },
        plugins: { legend: { labels: { color: '#ddd' } } },
    };

    new Chart(document.getElementById('k6ServerCpuChart'), {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: 'CPU %',
                data: ${JSON.stringify(cpu)},
                borderColor: '#ff6384',
                backgroundColor: 'rgba(255, 99, 132, 0.15)',
                fill: true,
                tension: 0.2,
            }],
        },
        options: { ...commonOptions, plugins: { ...commonOptions.plugins, title: { display: true, text: 'CPU', color: '#fff' } } },
    });

    new Chart(document.getElementById('k6ServerRamChart'), {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: 'RAM used MB',
                    data: ${JSON.stringify(ram)},
                    borderColor: '#36a2eb',
                    backgroundColor: 'rgba(54, 162, 235, 0.15)',
                    fill: true,
                    tension: 0.2,
                },
                {
                    label: 'RAM max MB',
                    data: ${JSON.stringify(ramMax)},
                    borderColor: '#9966ff',
                    borderDash: [6, 4],
                    fill: false,
                    tension: 0.2,
                },
            ],
        },
        options: { ...commonOptions, plugins: { ...commonOptions.plugins, title: { display: true, text: 'RAM', color: '#fff' } } },
    });

    new Chart(document.getElementById('k6ServerDiskChart'), {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: 'Disk MB/s',
                data: ${JSON.stringify(disk)},
                borderColor: '#4bc0c0',
                backgroundColor: 'rgba(75, 192, 192, 0.15)',
                fill: true,
                tension: 0.2,
            }],
        },
        options: { ...commonOptions, plugins: { ...commonOptions.plugins, title: { display: true, text: 'Disk I/O', color: '#fff' } } },
    });
})();
</script>
`;
}

export function injectIntoHtml(baseHtml, samples) {
    if (!baseHtml || !samples || samples.length === 0) {
        return baseHtml;
    }
    if (baseHtml.includes('<!-- k6-server-metrics -->')) {
        return baseHtml;
    }

    const injection = buildChartsInjection(samples);
    if (baseHtml.includes('</body>')) {
        return baseHtml.replace('</body>', `${injection}</body>`);
    }

    return `${baseHtml}${injection}`;
}
