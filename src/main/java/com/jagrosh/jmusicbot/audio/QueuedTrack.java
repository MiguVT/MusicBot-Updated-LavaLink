/*
 * Copyright 2021 John Grosh <john.a.grosh@gmail.com>.
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

import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.jagrosh.jmusicbot.queue.Queueable;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class QueuedTrack implements Queueable
{
    private final AudioTrack track;
    private final RequestMetadata requestMetadata;

    public QueuedTrack(AudioTrack track, RequestMetadata rm)
    {
        this.track = track;
        // Only set userData if it's not already set by the track implementation
        // YouTube tracks use userData for internal token storage
        if (track.getUserData() == null) {
            this.track.setUserData(rm == null ? RequestMetadata.EMPTY : rm);
        }

        this.requestMetadata = rm;
        if (this.track.isSeekable() && rm != null && rm.requestInfo != null)
            track.setPosition(rm.requestInfo.startTimestamp);
    }

    @Override
    public long getIdentifier()
    {
        return requestMetadata.getOwner();
    }

    public AudioTrack getTrack()
    {
        return track;
    }

    public RequestMetadata getRequestMetadata()
    {
        return requestMetadata;
    }

    @Override
    public String toString()
    {
        String entry = "`[" + TimeUtil.formatTime(track.getDuration()) + "]` ";
        AudioTrackInfo trackInfo = track.getInfo();
        entry = entry + (trackInfo.uri.startsWith("http") ? "[**" + trackInfo.title + "**]("+trackInfo.uri+")" : "**" + trackInfo.title + "**");
        // Use our stored requestMetadata instead of getUserData to avoid JSON parsing conflicts
        return entry + " - <@" + (requestMetadata != null ? requestMetadata.getOwner() : 0L) + ">";
    }
}
