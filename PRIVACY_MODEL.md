# Privacy Model — Guardian 0.1.7

## Core rule
Privacy Engine precedes Storage.

## PRIVATE
No package, app label or reason.

## SYSTEM
No package or app label.

## Other Android user/profile
The period is represented only as PRIVATE and UsageStats from that other user are not replayed into the owner's public history.

## Local questions
The 0.1.7 local question engine:
- reads sanitized local report data only;
- does not restore PRIVATE or SYSTEM identity;
- does not store the user's question;
- does not send the question anywhere;
- does not use Internet or an external model.

A question asking about a protected app cannot make Guardian reveal an identity that was never stored.

## Never collected
Screenshots/video, typed text, passwords, messages, notification contents, clipboard, banking content, Settings content, full URLs, page content, or identifiable other-user activity.
