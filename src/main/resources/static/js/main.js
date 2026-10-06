/**
 * ByteForce — Minimal Vanilla JavaScript interactions
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. Mobile Sidebar Drawer Toggle
  const sidebar = document.querySelector('.app-sidebar');
  const toggleBtn = document.querySelector('.mobile-nav-toggle');

  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener('click', () => {
      sidebar.classList.toggle('sidebar-open');
    });

    // Close when clicking outside on mobile
    document.addEventListener('click', (e) => {
      if (sidebar.classList.contains('sidebar-open') &&
          !sidebar.contains(e.target) &&
          !toggleBtn.contains(e.target)) {
        sidebar.classList.remove('sidebar-open');
      }
    });
  }

  // 2. Timed Assessment Engine
  const timerElement = document.getElementById('test-timer');
  const timerText = document.getElementById('test-timer-text');
  const testForm = document.getElementById('assessment-form');

  if (timerElement && testForm) {
    let durationSeconds = parseInt(timerElement.dataset.durationSeconds, 10) || 1800;

    const updateTimerDisplay = () => {
      if (durationSeconds <= 0) {
        if (timerText) {
          timerText.textContent = "00:00 - Time's up!";
        } else {
          timerElement.textContent = "00:00 - Time's up!";
        }
        timerElement.classList.add('timer-urgent');
        // Automatically submit the assessment
        testForm.submit();
        return;
      }

      const mins = Math.floor(durationSeconds / 60);
      const secs = durationSeconds % 60;
      const formatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
      if (timerText) {
        timerText.textContent = formatted;
      } else {
        timerElement.textContent = formatted;
      }

      if (durationSeconds <= 300) { // Under 5 minutes
        timerElement.classList.add('timer-urgent');
      }

      durationSeconds--;
    };

    updateTimerDisplay();
    setInterval(updateTimerDisplay, 1000);
  }

  // 3. Question Palette Switcher in Timed Assessment
  const qPanels = document.querySelectorAll('.test-question-card');
  const qButtons = document.querySelectorAll('.q-palette .q-btn');

  if (qPanels.length > 0 && qButtons.length > 0) {
    const showQuestion = (index) => {
      qPanels.forEach((p, idx) => {
        p.style.display = (idx === index) ? 'block' : 'none';
      });
      qButtons.forEach((b, idx) => {
        b.classList.toggle('active', idx === index);
      });
    };

    qButtons.forEach((btn) => {
      btn.addEventListener('click', () => {
        const targetIdx = parseInt(btn.dataset.index, 10);
        showQuestion(targetIdx);
      });
    });

    // Handle next/prev buttons inside questions
    document.querySelectorAll('[data-q-nav]').forEach(btn => {
      btn.addEventListener('click', () => {
        const offset = parseInt(btn.dataset.qNav, 10);
        let currentIdx = 0;
        qPanels.forEach((p, idx) => {
          if (p.style.display !== 'none') currentIdx = idx;
        });
        const nextIdx = Math.max(0, Math.min(qPanels.length - 1, currentIdx + offset));
        showQuestion(nextIdx);
      });
    });

    // Mark answered in palette when user interacts
    document.querySelectorAll('.test-question-card input, .test-question-card textarea').forEach(input => {
      input.addEventListener('change', () => {
        const card = input.closest('.test-question-card');
        if (card) {
          const idx = parseInt(card.dataset.index, 10);
          const btn = document.querySelector(`.q-palette .q-btn[data-index="${idx}"]`);
          if (btn) btn.classList.add('answered');
        }
      });
    });
  }

  // 4. Dark Mode Theme Toggle
  const initThemeToggle = () => {
    const savedTheme = localStorage.getItem('byteforce-theme') || (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    document.documentElement.setAttribute('data-theme', savedTheme);

    const toggleButtons = document.querySelectorAll('.theme-toggle-btn');
    toggleButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', nextTheme);
        localStorage.setItem('byteforce-theme', nextTheme);
      });
    });
  };
  initThemeToggle();
});

