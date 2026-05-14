CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON public.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id ON public.email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_panic_notification_results_alert_id ON public.panic_notification_results(alert_id);
CREATE INDEX IF NOT EXISTS idx_panic_notification_results_contact_id ON public.panic_notification_results(contact_id);
