# WorkLens Reporting System Notes

## Monthly rollup boundary

Monthly reports use the strict layered rollup model: a monthly report aggregates only weekly reports that have already been generated for that month.

The system does not create a month-end partial-week catch-up report from daily reports. As a known limitation, monthly report coverage can differ slightly from the natural calendar month when the month ends before the current natural week has been rolled up into a weekly report.
