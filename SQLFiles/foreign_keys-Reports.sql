ALTER TABLE IF EXISTS public.weekly_reports
    ADD CONSTRAINT fk_weekly_reports_user FOREIGN KEY (user_id)
    REFERENCES public.users (user_id) MATCH SIMPLE ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.weekly_reports
    ADD CONSTRAINT fk_weekly_reports_song FOREIGN KEY (top_song_id)
    REFERENCES public.songs (song_id) MATCH SIMPLE ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.weekly_reports
    ADD CONSTRAINT fk_weekly_reports_artist FOREIGN KEY (top_artist_id)
    REFERENCES public.artists (artist_id) MATCH SIMPLE ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.weekly_reports
    ADD CONSTRAINT fk_weekly_reports_album FOREIGN KEY (top_album_id)
    REFERENCES public.songs (song_id) MATCH SIMPLE ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.weekly_reports
    ADD CONSTRAINT fk_weekly_reports_genre FOREIGN KEY (top_genre_id)
    REFERENCES public.genres (genre_id) MATCH SIMPLE ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.monthly_reports
    ADD CONSTRAINT fk_monthly_reports_user FOREIGN KEY (user_id)
    REFERENCES public.users (user_id) MATCH SIMPLE ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.monthly_reports
    ADD CONSTRAINT fk_monthly_reports_song FOREIGN KEY (top_song_id)
    REFERENCES public.songs (song_id) MATCH SIMPLE ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.monthly_reports
    ADD CONSTRAINT fk_monthly_reports_artist FOREIGN KEY (top_artist_id)
    REFERENCES public.artists (artist_id) MATCH SIMPLE ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.monthly_reports
    ADD CONSTRAINT fk_monthly_reports_album FOREIGN KEY (top_album_id)
    REFERENCES public.songs (song_id) MATCH SIMPLE ON DELETE SET NULL;

ALTER TABLE IF EXISTS public.monthly_reports
    ADD CONSTRAINT fk_monthly_reports_genre FOREIGN KEY (top_genre_id)
    REFERENCES public.genres (genre_id) MATCH SIMPLE ON DELETE SET NULL;