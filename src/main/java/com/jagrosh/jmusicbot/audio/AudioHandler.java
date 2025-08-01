/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jmusicbot.queue.AbstractQueue;
import com.jagrosh.jmusicbot.settings.QueueType;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.jagrosh.jmusicbot.settings.RepeatMode;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler
{
    public final static String PLAY_EMOJI  = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI  = "\u23F9"; // ⏹


    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = new HashSet<>();

    // Track metadata mapping to avoid userData conflicts with YouTube tracks
    private final Map<String, RequestMetadata> trackMetadata = new ConcurrentHashMap<>();

    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;

    private AudioFrame lastFrame;
    private AbstractQueue<QueuedTrack> queue;

    // SponsorBlock integration
    private List<SponsorBlockClient.Segment> sponsorSegments = new LinkedList<>();
    private int currentSegmentIndex = 0;
    private SponsorBlockClient sponsorBlockClient = new SponsorBlockClient();

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player)
    {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();

        this.setQueueType(manager.getBot().getSettingsManager().getSettings(guildId).getQueueType());
    }

    public void setQueueType(QueueType type)
    {
        queue = type.createInstance(queue);
    }

    public int addTrackToFront(QueuedTrack qtrack)
    {
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
        {
            queue.addAt(0, qtrack);
            return 0;
        }
    }

    public int addTrack(QueuedTrack qtrack)
    {
        // Store metadata separately to avoid userData conflicts
        if (qtrack.getRequestMetadata() != null) {
            trackMetadata.put(qtrack.getTrack().getIdentifier(), qtrack.getRequestMetadata());
        }

        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
            return queue.add(qtrack);
    }

    public AbstractQueue<QueuedTrack> getQueue()
    {
        return queue;
    }

    public void stopAndClear()
    {
        queue.clear();
        defaultQueue.clear();
        audioPlayer.stopTrack();
        //current = null;
    }

    public boolean isMusicPlaying(JDA jda)
    {
        return guild(jda).getSelfMember().getVoiceState().inAudioChannel() && audioPlayer.getPlayingTrack()!=null;
    }

    public Set<String> getVotes()
    {
        return votes;
    }

    public AudioPlayer getPlayer()
    {
        return audioPlayer;
    }

    public RequestMetadata getRequestMetadata()
    {
        if(audioPlayer.getPlayingTrack() == null)
            return RequestMetadata.EMPTY;

        // Get metadata from our safe mapping instead of userData
        RequestMetadata metadata = trackMetadata.get(audioPlayer.getPlayingTrack().getIdentifier());
        return metadata != null ? metadata : RequestMetadata.EMPTY;
    }    public boolean playFromDefault()
    {
        if(!defaultQueue.isEmpty())
        {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if(settings==null || settings.getDefaultPlaylist()==null)
            return false;

        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(settings.getDefaultPlaylist());
        if(pl==null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (at) ->
        {
            if(audioPlayer.getPlayingTrack()==null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () ->
        {
            if(pl.getTracks().isEmpty() && !manager.getBot().getConfig().getStay())
            {
                // Log playFromDefault disconnect
                org.slf4j.LoggerFactory.getLogger("AudioHandler").warn("VOICE DISCONNECT: playFromDefault callback - playlist empty and stayinchannel=false for guild {}", guildId);
                manager.getBot().closeAudioConnection(guildId);
            }
        });
        return true;
    }

    // Audio Events
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason)
    {
        RepeatMode repeatMode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        // if the track ended normally, and we're in repeat mode, re-add it to the queue
        if(endReason==AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF)
        {
            // Get metadata from our safe mapping
            RequestMetadata trackMetadata = this.trackMetadata.get(track.getIdentifier());
            if (trackMetadata == null) {
                trackMetadata = RequestMetadata.EMPTY;
            }

            QueuedTrack clone = new QueuedTrack(track.makeClone(), trackMetadata);
            if(repeatMode == RepeatMode.ALL)
                queue.add(clone);
            else
                queue.addAt(0, clone);
        }

        if(queue.isEmpty())
        {
            if(!playFromDefault())
            {
                manager.getBot().getNowplayingHandler().onTrackUpdate(null);
                if(!manager.getBot().getConfig().getStay())
                {
                    // Log onTrackEnd disconnect
                    org.slf4j.LoggerFactory.getLogger("AudioHandler").warn("VOICE DISCONNECT: onTrackEnd - queue empty, no default playlist, and stayinchannel=false for guild {}", guildId);
                    manager.getBot().closeAudioConnection(guildId);
                }
                // unpause, in the case when the player was paused and the track has been skipped.
                // this is to prevent the player being paused next time it's being used.
                player.setPaused(false);
            }
        }
        else
        {
            QueuedTrack qt = queue.pull();
            player.playTrack(qt.getTrack());
        }

        // Clean up metadata for finished track to prevent memory leaks
        if (track != null) {
            trackMetadata.remove(track.getIdentifier());
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LoggerFactory.getLogger("AudioHandler").error("Track " + track.getIdentifier() + " has failed to play", exception);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track)
    {
        votes.clear();
        manager.getBot().getNowplayingHandler().onTrackUpdate(track);

        // SponsorBlock: fetch segments asynchronously if YouTube track
        sponsorSegments.clear();
        currentSegmentIndex = 0;
        if (track.getSourceManager() != null && track.getSourceManager().getClass().getSimpleName().toLowerCase().contains("youtube")) {
            String videoId = track.getIdentifier();
            sponsorBlockClient.fetchSegmentsAsync(videoId).thenAccept(segments -> {
                // Only update if the same track is still playing
                if (audioPlayer.getPlayingTrack() != null && audioPlayer.getPlayingTrack().getIdentifier().equals(videoId)) {
                    sponsorSegments = segments;
                    currentSegmentIndex = 0;
                }
            });
        }
    }


    // Formatting
    public MessageCreateData getNowPlaying(JDA jda)
    {
        if(isMusicPlaying(jda))
        {
            Guild guild = guild(jda);
            AudioTrack track = audioPlayer.getPlayingTrack();
            MessageCreateBuilder mb = new MessageCreateBuilder();
            mb.setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess()+" **Now Playing in "+guild.getSelfMember().getVoiceState().getChannel().getAsMention()+"...**"));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(guild.getSelfMember().getColor());
            RequestMetadata rm = getRequestMetadata();
            if(rm.getOwner() != 0L)
            {
                User u = guild.getJDA().getUserById(rm.user.id);
                if(u==null)
                    eb.setAuthor(FormatUtil.formatUsername(rm.user), null, rm.user.avatar);
                else
                    eb.setAuthor(FormatUtil.formatUsername(u), null, u.getEffectiveAvatarUrl());
            }

            try
            {
                eb.setTitle(track.getInfo().title, track.getInfo().uri);
            }
            catch(Exception e)
            {
                eb.setTitle(track.getInfo().title);
            }

            if(track instanceof YoutubeAudioTrack && manager.getBot().getConfig().useNPImages())
            {
                eb.setThumbnail("https://img.youtube.com/vi/"+track.getIdentifier()+"/mqdefault.jpg");
            }

            if(track.getInfo().author != null && !track.getInfo().author.isEmpty())
                eb.setFooter("Source: " + track.getInfo().author, null);

            double progress = (double)audioPlayer.getPlayingTrack().getPosition()/track.getDuration();
            eb.setDescription(getStatusEmoji()
                    + " "+FormatUtil.progressBar(progress)
                    + " `[" + TimeUtil.formatTime(track.getPosition()) + "/" + TimeUtil.formatTime(track.getDuration()) + "]` "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume()));

            return mb.setEmbeds(eb.build()).build();
        }
        else return null;
    }

    public MessageCreateData getNoMusicPlaying(JDA jda)
    {
        Guild guild = guild(jda);
        return new MessageCreateBuilder()
                .setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess()+" **Now Playing...**"))
                .setEmbeds(new EmbedBuilder()
                .setTitle("No music playing")
                .setDescription(STOP_EMOJI+" "+FormatUtil.progressBar(-1)+" "+FormatUtil.volumeIcon(audioPlayer.getVolume()))
                .setColor(guild.getSelfMember().getColor())
                .build()).build();
    }

    public String getStatusEmoji()
    {
        return audioPlayer.isPaused() ? PAUSE_EMOJI : PLAY_EMOJI;
    }

    // Audio Send Handler methods
    /*@Override
    public boolean canProvide()
    {
        if (lastFrame == null)
            lastFrame = audioPlayer.provide();

        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio()
    {
        if (lastFrame == null)
            lastFrame = audioPlayer.provide();

        byte[] data = lastFrame != null ? lastFrame.getData() : null;
        lastFrame = null;

        return data;
    }*/

    @Override
    public boolean canProvide()
    {
        lastFrame = audioPlayer.provide();
        // SponsorBlock: skip segments if needed
        if (audioPlayer.getPlayingTrack() != null && !sponsorSegments.isEmpty()) {
            long positionMs = audioPlayer.getPlayingTrack().getPosition();
            while (currentSegmentIndex < sponsorSegments.size()) {
                SponsorBlockClient.Segment seg = sponsorSegments.get(currentSegmentIndex);
                long segStart = (long)(seg.start * 1000);
                long segEnd = (long)(seg.end * 1000);
                if (positionMs >= segStart && positionMs < segEnd) {
                    audioPlayer.getPlayingTrack().setPosition(segEnd);
                    currentSegmentIndex++;
                    // After seeking, update position
                    positionMs = audioPlayer.getPlayingTrack().getPosition();
                } else if (positionMs >= segEnd) {
                    currentSegmentIndex++;
                } else {
                    break;
                }
            }
        }
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio()
    {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus()
    {
        return true;
    }


    // Private methods
    private Guild guild(JDA jda)
    {
        return jda.getGuildById(guildId);
    }
}
