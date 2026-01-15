/**
 * Haptic Feedback Utility
 * Provides tactile feedback for mobile interactions
 */

const HapticFeedback = {
    // Check if Vibration API is supported
    isSupported: function () {
        return 'vibrate' in navigator;
    },

    // Vibration patterns (in milliseconds)
    patterns: {
        light: 10,      // Light tap - for card taps, links
        medium: 20,     // Medium tap - for button presses
        success: 30,    // Success - for successful actions
        error: [10, 50, 10], // Error pattern - short-pause-short
        selection: 5    // Very light - for selections/toggles
    },

    // Trigger haptic feedback
    trigger: function (pattern) {
        if (!this.isSupported()) {
            return; // Gracefully degrade if not supported
        }

        try {
            const vibrationPattern = this.patterns[pattern] || this.patterns.light;
            navigator.vibrate(vibrationPattern);
        } catch (error) {
            console.warn('Haptic feedback failed:', error);
        }
    },

    // Convenience methods
    light: function () {
        this.trigger('light');
    },

    medium: function () {
        this.trigger('medium');
    },

    success: function () {
        this.trigger('success');
    },

    error: function () {
        this.trigger('error');
    },

    selection: function () {
        this.trigger('selection');
    }
};

// Auto-attach haptic feedback to common interactive elements
document.addEventListener('DOMContentLoaded', function () {
    // Service cards
    document.querySelectorAll('.service-card').forEach(card => {
        card.addEventListener('touchstart', function () {
            HapticFeedback.light();
        }, { passive: true });
    });

    // Stat cards (carousel)
    document.querySelectorAll('.stat-card').forEach(card => {
        card.addEventListener('touchstart', function () {
            HapticFeedback.light();
        }, { passive: true });
    });

    // Primary buttons
    document.querySelectorAll('.btn-primary, .btn-dark').forEach(button => {
        button.addEventListener('touchstart', function () {
            HapticFeedback.medium();
        }, { passive: true });
    });

    // Links
    document.querySelectorAll('a:not(.service-card)').forEach(link => {
        link.addEventListener('touchstart', function () {
            HapticFeedback.light();
        }, { passive: true });
    });

    // Form inputs (on focus)
    document.querySelectorAll('input, select, textarea').forEach(input => {
        input.addEventListener('focus', function () {
            HapticFeedback.selection();
        }, { passive: true });
    });

    // Checkboxes and radio buttons
    document.querySelectorAll('input[type="checkbox"], input[type="radio"]').forEach(input => {
        input.addEventListener('change', function () {
            HapticFeedback.selection();
        });
    });

    // OTP inputs
    document.querySelectorAll('.otp-inputs input').forEach(input => {
        input.addEventListener('input', function () {
            if (this.value.length === 1) {
                HapticFeedback.selection();
            }
        });
    });

    // Form submissions (success feedback)
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', function () {
            HapticFeedback.medium();
        });
    });
});

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = HapticFeedback;
}
