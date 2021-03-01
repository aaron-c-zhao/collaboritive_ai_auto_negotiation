package collabai.group42;

import collabai.group42.acceptance.AcceptanceStrategy;
import collabai.group42.biddingStrategy.BiddingStrategy;
import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import geniusweb.boa.BoaParty;
import geniusweb.boa.InstantiationFailedException;


import geniusweb.inform.Settings;
import geniusweb.issuevalue.Bid;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.Profile;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import tudelft.utilities.logging.Reporter;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the current state of the negotiation for a {@link BoaParty}, including
 * the state of the opponent models. immutable.
 *
 */
public class BoaState {
	private final Settings settings;
	private final Profile profile;
	private final Progress progress;
	private final Class<? extends OpponentModel> opponentModelClass;
	private final Map<PartyId, OpponentModel> opponentModels;
	private final List<Action> actionHistory;
	private final BiddingStrategy biddingStrategy;
	private final AcceptanceStrategy acceptanceStrategy;
	private final Reporter reporter;

	/**
	 * Initial state. Update using with(settings).
	 * 
	 * @param reporter the {@link Reporter} to use for logging
	 */
	public BoaState(Reporter reporter) {
		this(null, null, null, null, null, null, Collections.emptyList(),
				Collections.emptyMap(), reporter);
		if (reporter == null) {
			throw new NullPointerException("reporter must be not null");
		}
	}

	/**
	 * Internal constructor. Use
	 *
	 * @param settings      the negoi {@link Settings}
	 * @param profile       the {@link Profile} to be used
	 * @param progr         the {@link Progress}
	 * @param bidstrat      the {@link BiddingStrategy}
	 * @param acceptstrat   the {@link AcceptanceStrategy}
	 * @param omClass       the class of the {@link OpponentModel}
	 * @param actionHistory list of {@link Action}s that have been done in the
	 *                      negotiatino (by us and others), first action is the
	 *                      oldest.
	 * @param oppMods       a map of {@link OpponentModel}s, one for each
	 *                      {@link PartyId} encountered in the negotiation
	 * @param reporter      the {@link Reporter} that can be used to log
	 *                      messages. Should equal {@link BoaParty}'s reporter
	 */
	private BoaState(Settings settings, Profile profile, Progress progr,
                     BiddingStrategy bidstrat, AcceptanceStrategy acceptstrat,
                     Class<? extends OpponentModel> omClass, List<Action> actionHistory,
                     Map<PartyId, OpponentModel> oppMods, Reporter reporter) {
		this.settings = settings;
		this.profile = profile;
		this.progress = progr;
		this.biddingStrategy = bidstrat;
		this.acceptanceStrategy = acceptstrat;
		this.opponentModelClass = omClass;
		this.actionHistory = actionHistory;
		this.opponentModels = oppMods;
		this.reporter = reporter;
	}

	/**
	 * This should be the first update call used.
	 * 
	 * @param newsettings the negoi {@link Settings}
	 * @param bidstrat    the {@link BiddingStrategy}
	 * @param acceptstrat the {@link AcceptanceStrategy}
	 * @param omClass     the class of the {@link OpponentModel}
	 * @return initialized BoaState
	 */
	public BoaState with(Settings newsettings, BiddingStrategy bidstrat,
			AcceptanceStrategy acceptstrat,
			Class<? extends OpponentModel> omClass) {
		if (settings != null)
			throw new IllegalStateException("settings already set");
		if (newsettings == null || omClass == null || bidstrat == null
				|| acceptstrat == null || reporter == null)
			throw new NullPointerException(
					"settings, reporter, bidstrat, acceptstrat, omClass and oModels must be not null");
		return new BoaState(newsettings, null, newsettings.getProgress(),
				bidstrat, acceptstrat, omClass, actionHistory,
				new HashMap<PartyId, OpponentModel>(), reporter);
	}

	public BoaState with(Profile newprofile) {
		return new BoaState(settings, newprofile, progress, biddingStrategy,
				acceptanceStrategy, opponentModelClass, actionHistory,
				opponentModels, reporter);

	}

	/**
	 * 
	 * @return the settings for this negotiation, includes my {@link PartyId},
	 *         {@link Parameter}, {@link Profile} etc.
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * 
	 * @return the {@link Profile} as fetched from the profiles server, or null
	 *         if no profile was received yet.
	 */
	public Profile getProfile() {
		return profile;
	}

	/**
	 * 
	 * @param action the action that was done by some participant (possibly us).
	 * @return new BoaState that includes action.getBid as last bid (if action
	 *         contains a bid), and with updated opponentmodel of that
	 *         participant.
	 * @throws InstantiationFailedException if one of provided classes can not
	 *                                      be instantiated.
	 */
	public BoaState with(Action action) throws InstantiationFailedException {
		List<Action> newactions = new ArrayList<Action>(actionHistory);
		newactions.add(action);
		Progress newprogress = progress;
		Map<PartyId, OpponentModel> newmodels = opponentModels;

		if (action.getActor().equals(settings.getID())) {
			// ourside, just advance progress
			if (progress instanceof ProgressRounds) {
				// bit hacky, maybe we should wait
				newprogress = ((ProgressRounds) progress).advance();
			}
		} else {
			// not ourselves, update the OpponentModel
			newmodels = updateModels(action);
		}
		return new BoaState(settings, profile, newprogress, biddingStrategy,
				acceptanceStrategy, opponentModelClass, newactions, newmodels,
				reporter);
	}

	/**
	 * @return the current {@link Progress}
	 */
	public Progress getProgress() {
		return progress;
	}

	/**
	 * @return list of actions done so far, oldest action first
	 */
	public List<Action> getActionHistory() {
		return Collections.unmodifiableList(actionHistory);
	}

	/**
	 * 
	 * @param bid the {@link Bid} to check
	 * @return true iff {@link #acceptanceStrategy} says the bid is acceptable
	 */
	public boolean isAcceptable(Bid bid) {
		return acceptanceStrategy.isAcceptable(bid, this);
	}

	/**
	 * 
	 * @return the next recommended action according to the
	 *         {@link #biddingStrategy}
	 */
	public Action getAction() {
		return biddingStrategy.getAction(this);
	}

	/**
	 * 
	 * @return the Reporter (same as the BoaParty's reporter)
	 */
	public Reporter getReporter() {
		return reporter;
	}

	/**
	 * @param action
	 * @return the map with the opponentmodels, with the model for the party
	 *         that did the action updated.
	 * @throws InstantiationFailedException
	 */
	private Map<PartyId, OpponentModel> updateModels(Action action)
			throws InstantiationFailedException {
		PartyId actor = action.getActor();
		Map<PartyId, OpponentModel> updated = extendedOpponentModels(actor);
		updated.put(actor, updated.get(actor).with(action, progress));
		return updated;
	}

	/**
	 * Get the current opponent model, extended with newparty if there is no
	 * model yet for newparty.
	 * 
	 * @param newparty a possibly new party
	 * @return a (possibly new) opponentModels map
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InstantiationFailedException
	 */
	private Map<PartyId, OpponentModel> extendedOpponentModels(PartyId newparty)
			throws InstantiationFailedException {
		// check if this is new party
		if (opponentModels.containsKey(newparty))
			return opponentModels;

		Map<PartyId, OpponentModel> newmodels = new HashMap<>(opponentModels);
		OpponentModel newmodel;
		try {
			newmodel = opponentModelClass.newInstance()
					.with(profile.getDomain(), profile.getReservationBid());
		} catch (Exception e) {
			throw new InstantiationFailedException(
					"Failed to instantiate " + opponentModelClass, e);
		}
		newmodels.put(newparty, newmodel);
		return newmodels;
	}

}
