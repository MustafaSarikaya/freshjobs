-- Several companies have moved job boards since they were seeded. Old
-- listings stay attached to the source and are expired on the next run
-- because their external IDs no longer appear in the feed.

-- HubSpot's Greenhouse board token changed from "hubspot" to "hubspotjobs".
UPDATE core_source SET board_id = 'hubspotjobs', updated_at = now()
WHERE platform = 'greenhouse' AND board_id = 'hubspot'
  AND NOT EXISTS (SELECT 1 FROM core_source
                  WHERE platform = 'greenhouse' AND board_id = 'hubspotjobs');

-- Vercel moved from Ashby to Greenhouse.
UPDATE core_source SET platform = 'greenhouse', updated_at = now()
WHERE platform = 'ashby' AND board_id = 'vercel'
  AND NOT EXISTS (SELECT 1 FROM core_source
                  WHERE platform = 'greenhouse' AND board_id = 'vercel');

-- Atlassian moved from Lever to iCIMS and Deel to its own ATS; neither has an
-- adapter yet, so stop polling them rather than failing every run.
UPDATE core_source SET is_active = false, updated_at = now()
WHERE (platform = 'lever' AND board_id = 'atlassian')
   OR (platform = 'ashby' AND board_id = 'deel');
