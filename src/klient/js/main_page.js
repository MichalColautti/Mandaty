/**
 * Obsługa wyświetlania mandatów przez klienta w main_page
 */
document.addEventListener('DOMContentLoaded', () => {
    const pesel = sessionStorage.getItem('pesel');
    if (!pesel) {
        // Przekierowanie do strony głównej przy braku numeru Pesel
        window.location.href = '/';
        return;
    }

    // Wysłanie POST z PESEL
    fetch('/api', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            action: 'main_page',  // Dodajemy action w body
            pesel: pesel
        })
    })
        .then(response => response.json())
        .then(tickets => {
            const container = document.getElementById('tickets-container');

            // Usuwanie napisu "Ładowanie mandatów..."
            container.innerHTML = '';

            if (tickets.length === 0) {
                container.innerHTML = '<p>Brak mandatów.</p>';
                return;
            }

            // Wyświetlanie mandatów
            tickets.forEach(ticket => {
                const ticketDiv = document.createElement('div');
                ticketDiv.classList.add('ticket');
                ticketDiv.innerHTML = `
                <p><strong>Imię i nazwisko:</strong> ${ticket.driver_name}</p>
                <p><strong>Wykroczenie:</strong> ${ticket.offense}</p>
                <p><strong>Kwota mandatu:</strong> ${ticket.fine_amount} zł</p>
                <p><strong>Punkty karne:</strong> ${ticket.penalty_points}</p>
                <p><strong>Data wystawienia:</strong> ${ticket.issue_date}</p>
            `;
                container.appendChild(ticketDiv);
            });
        })
        .catch(err => {
            console.error('Błąd podczas ładowania mandatów:', err);
            const container = document.getElementById('tickets-container');
            container.innerHTML = '<p>Błąd podczas ładowania mandatów.</p>';
        });
});