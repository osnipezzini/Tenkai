/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2server.gameserver.handler.dailymissionhandlers;


import l2server.gameserver.handler.AbstractDailyMissionHandler;
import l2server.gameserver.model.DailyMissionDataHolder;
import l2server.gameserver.model.DailyMissionPlayerEntry;
import l2server.gameserver.model.DailyMissionStatus;
import l2server.gameserver.model.actor.instance.L2PcInstance;
import l2server.gameserver.model.event.Containers;
import l2server.gameserver.model.event.EventType;
import l2server.gameserver.model.event.impl.creature.player.OnPlayerFishing;
import l2server.gameserver.model.event.listeners.ConsumerEventListener;
import l2server.gameserver.network.serverpackets.ExFishingEnd;

public class FishingDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	
	public FishingDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 0);
		_maxLevel = holder.getParams().getInt("maxLevel", Byte.MAX_VALUE);
	}
	
	@Override
	public void init()
	{
		Containers.Players().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_FISHING, (OnPlayerFishing event) -> onPlayerFishing(event), this));
	}
	
	@Override
	public boolean isAvailable(L2PcInstance player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		if (entry != null)
		{
			switch (entry.getStatus())
			{
				case NOT_AVAILABLE: // Initial state
				{
					if (entry.getProgress() >= _amount)
					{
						entry.setStatus(DailyMissionStatus.AVAILABLE);
						storePlayerEntry(entry);
					}
					break;
				}
				case AVAILABLE:
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private void onPlayerFishing(OnPlayerFishing event)
	{
		final L2PcInstance player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel))
		{
			return;
		}
		
		if (event.getReason() == ExFishingEnd.FishingEndReason.WIN)
		{
			final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);
			if (entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE)
			{
				if (entry.increaseProgress() >= _amount)
				{
					entry.setStatus(DailyMissionStatus.AVAILABLE);
				}
				storePlayerEntry(entry);
			}
		}
	}
}
