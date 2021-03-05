package collabai.group42;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.boa.InstantiationFailedException;
import collabai.group42.acceptance.AcceptanceStrategy;
import collabai.group42.acceptance.NextAcceptanceStrategy;
import collabai.group42.biddingStrategy.BiddingStrategy;
import geniusweb.inform.ActionDone;
import geniusweb.inform.Finished;
import geniusweb.inform.Inform;
import geniusweb.inform.Settings;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.party.Capabilities;
import geniusweb.party.DefaultParty;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import tudelft.utilities.logging.Reporter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 * Default behaviour for Boa-style parties
 */
public abstract class DefaultBoa extends DefaultParty {

    private BoaState negoState;
    private ProfileInterface profileint = null;

    public DefaultBoa() {
        super();
        negoState = initialState();
    }

    public DefaultBoa(Reporter reporter) {
        super(reporter); // for debugging
        negoState = initialState();
    }

    /**
     * This only supports SAOP protocol because it is not clear how the
     * BiddingStrategy could be made more general without basically becoming a
     * full Party.
     */
    @Override
    public Capabilities getCapabilities() {
        /*
         * At this moment only SAOP is supported by BOA. To support others, the
         * components also need to get capabilities. FIXME limiting this to
         * linearadditive may be overly restrictive #1871
         */
        return new Capabilities(new HashSet<>(Arrays.asList("SAOP")),
                Collections.singleton(LinearAdditive.class));
    }

    /**
     * Impementation of generic BOA behaviour.
     */
    @Override
    public void notifyChange(Inform info) {
        try {
            if (info instanceof Settings) {
                /*
                 * this should be first call. We can not yet create the state
                 * because we need to wait for the profile fetch.
                 */
                Settings settings = (Settings) info;
                Class<? extends OpponentModel> omClass = getOpponentModel(
                        settings);
                BiddingStrategy bidStrat = getBiddingStrategy(settings);
                AcceptanceStrategy acceptStrat = getAccceptanceStrategy(
                        settings);
                negoState = negoState.with(settings, bidStrat, acceptStrat,
                        omClass);
                this.profileint = ProfileConnectionFactory
                        .create(settings.getProfile().getURI(), getReporter());
                return;
            }

            // Beyond this point we should have a profile
            if (negoState.getProfile() == null) {
                negoState = negoState.with(this.profileint.getProfile());
            }

            if (info instanceof ActionDone) {
                negoState = negoState.with(((ActionDone) info).getAction());
            } else if (info instanceof YourTurn) {
                getConnection().send(getAction());
                // we will receive ActionDone and update state then
            } else if (info instanceof Finished) {
                getReporter().log(Level.INFO, "Final ourcome:" + info);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle info " + info, e);
        }

    }

    /**
     * Overridable getter for the acceptance strategy. Override if you want to
     * hard code a {@link Group42Party} behaviour
     *
     * @param settings the {@link Settings}
     * @return the {@link OpponentModel} class to use for modeling opponents
     * @throws InstantiationFailedException if the requested opponent model can
     *                                      not be loaded
     */
    abstract protected Class<? extends OpponentModel> getOpponentModel(
            Settings settings) throws InstantiationFailedException;

    /**
     * Overridable getter for the acceptance strategy. Override if you want to
     * hard code a {@link Group42Party} behaviour
     *
     * @param settings the {@link Settings}
     * @return the {@link BiddingStrategy} to use for determining the next
     * {@link Action}
     * @throws InstantiationFailedException if the requested opponent model can
     *                                      not be loaded
     */
    abstract protected BiddingStrategy getBiddingStrategy(Settings settings)
            throws InstantiationFailedException;

    /**
     * Overridable getter for the acceptance strategy. Override if you want to
     * hard code a {@link BoaState} behaviour
     *
     * @param settings the {@link Settings}
     * @return the {@link AcceptanceStrategy} to use for determining if a bid is
     * acceptable
     * @throws InstantiationFailedException if the requested opponent model can
     *                                      not be loaded
     */
    abstract protected AcceptanceStrategy getAccceptanceStrategy(
            Settings settings) throws InstantiationFailedException;

    /**
     * Factory method to create initial state. Override to insert mock
     *
     * @return initial BoaState
     */
    protected BoaState initialState() {
        return new BoaState(getReporter());
    }

    /**
     * @return the most recent bid that was offered, or null if no offer has
     * been done yet.
     */
    protected Bid getLastBid() {
        List<Action> actions = negoState.getActionHistory();
        for (int n = actions.size() - 1; n >= 0; n--) {
            Action action = actions.get(n);
            if (action instanceof Offer) {
                return ((Offer) action).getBid();
            }
        }
        return null;
    }

    private Action getAction() {
        Bid lastBid = getLastBid();
        if (lastBid != null && negoState.isAcceptable(lastBid)) {
            return new Accept(negoState.getSettings().getID(), lastBid);
        }
        return negoState.getAction();
    }

}
