# Risk Fusion

The backend fuses conversation and visual signals using:
- confidence
- signal combinations
- temporal accumulation
- conversation stage

Current combination examples:
- authority + urgency
- urgency + financial request
- financial request + OTP request
- isolation + financial request

Levels:
`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`, `UNKNOWN`.

The output is an explainable risk update, not a claim that the other participant is a scammer.
