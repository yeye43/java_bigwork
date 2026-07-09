package edu.jieqi.ai;

import edu.jieqi.engine.Board;
import edu.jieqi.engine.MoveGenerator;
import edu.jieqi.engine.RuleEngine;
import edu.jieqi.model.Move;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class SearchAi {
    private static final int MAX_DEPTH = 5;
    private static final int NORMAL_MAX_DEPTH = 4;
    private static final int WIN_SCORE = 1_000_000;
    private static final int CHECK_BONUS = 1_350;
    private static final int SELF_CHECK_PENALTY = 4_000;
    private static final int REVEAL_BONUS = 680;
    private static final int ATTACK_BONUS = 24;
    private static final int MAX_BRANCHING = 36;
    private static final int NON_CRITICAL_MAX_BRANCHING = 28;
    private static final int STOCHASTIC_MARGIN = 180;
    private static final int BAD_EXCHANGE_PENALTY = 5;
    private static final int HIGH_VALUE_PIECE = 420;
    private static final int QUIESCENCE_DEPTH = 3;
    private static final int QUIESCENCE_BRANCHING = 18;
    private static final int NON_CRITICAL_QUIESCENCE_BRANCHING = 12;
    private static final int EXTENSION_LIMIT = 2;
    private static final int TRANSPOSITION_LIMIT = 120_000;
    private static final int HISTORY_LIMIT = 18_000;
    private static final int NULL_MOVE_MIN_DEPTH = 3;
    private static final int NULL_MOVE_REDUCTION = 2;
    private static final int NULL_MOVE_MIN_MATERIAL = 1_200;
    private static final int IID_MIN_DEPTH = 4;
    private static final int HIDDEN_EXPECT_PLY_LIMIT = 2;
    private static final int LMR_MIN_DEPTH = 3;
    private static final int LMR_MOVE_INDEX = 8;
    private static final int FUTILITY_MARGIN_BASE = 380;
    private static final int[] FUTILITY_MARGINS = {0, 380, 720, 1_200};
    private static final int PASSIVE_KING_MOVE_PENALTY = 4_800;
    private static final int XIANGQI_KNOWLEDGE_START_PHASE = 18;
    private static final int XIANGQI_KNOWLEDGE_FULL_PHASE = 68;
    private static final int REVEAL_QUIESCENCE_PHASE = 55;
    private static final int REPLY_THREAT_PENALTY = 2;
    private static final int FOLLOWUP_THREAT_BONUS = 3;
    private static final int ROOTED_EXCHANGE_BONUS = 180;
    private static final int CANNON_LINE_PRESSURE_BONUS = 260;
    private static final int ROOK_RIB_PRESSURE_BONUS = 170;
    private static final int EXPOSED_MAJOR_PIECE_PENALTY = 1_250;
    private static final int EXPOSED_NON_PAWN_PENALTY = 950;
    private static final int IDLE_KING_MOVE_EXTRA_PENALTY = 2_600;
    private static final int CAPTURE_THREATENING_PIECE_BONUS = 4_800;
    private static final int THREATENED_MAJOR_ESCAPE_BONUS = 2_200;
    private static final int HOME_DEFENSE_CAPTURE_BONUS = 12_000;
    private static final int INVADING_MAJOR_PENALTY = 2_800;
    private static final int HIDDEN_CAPTURE_BONUS = 1_400;
    private static final int FORCED_TACTICAL_LOSS_PENALTY = 900_000;
    private static final int KING_RECAPTURE_MAJOR_PENALTY = 12_000;
    private static final int LATE_MAJOR_NON_PROGRESS_PENALTY = 1_600;
    private static final int LOW_VALUE_CHECK_PENALTY = 2_400;
    private static final int ENDGAME_REVEAL_BONUS = 1_050;
    private static final int KING_ATTACK_PROGRESS_BONUS = 1_250;
    private static final int ENDGAME_MATE_NET_PROGRESS_BONUS = 1_600;
    private static final int ACTIVE_BREAKTHROUGH_BONUS = 900;
    private static final int FLYING_KING_COORDINATION_BONUS = 1_100;
    private static final int OWN_PALACE_BLOCK_PENALTY = 3_800;
    private static final int KING_ESCAPE_LOSS_PENALTY = 3_200;
    private static final int PALACE_UNBLOCK_BONUS = 3_400;
    private static final int ACTIVE_PIECE_ACTIVATION_BONUS = 1_900;
    private static final int HOME_INVADER_PENALTY = 1_150;
    private static final int DEFENSIVE_MAJOR_RELIEF_BONUS = 1_650;
    private static final int OPPONENT_PLAN_THREAT_BONUS = 1_850;
    private static final int NON_DECISIVE_CHECK_PENALTY = 1_900;
    private static final int AIMLESS_HARASS_PENALTY = 1_650;
    private static final int UNSOUND_CHECK_SACRIFICE_PENALTY = 5_800;
    private static final int COORDINATED_ATTACK_BONUS = 1_350;
    private static final int NEXT_MOVE_MAJOR_LOSS_PENALTY = 7_500;
    private static final int PAWN_TRAP_BONUS = 1_450;
    private static final int PAWN_CONSTRICTION_PROGRESS_BONUS = 1_150;
    private static final int CANNON_UNLOCK_BONUS = 1_250;
    private static final int CANNON_SCREEN_PROGRESS_BONUS = 1_300;
    private static final int KNIGHT_KILL_SHAPE_PROGRESS_BONUS = 1_250;
    private static final int AIMLESS_REPEAT_CHECK_EXTRA_PENALTY = 3_800;
    private static final int UNCOMPENSATED_SACRIFICE_PENALTY = 6_200;
    private static final int UNCERTAIN_RECAPTURE_PENALTY = 3_600;
    private static final int FORMATION_PROGRESS_BONUS = 1_200;
    private static final int OBVIOUS_GIFT_PENALTY = 5_400;
    private static final int RECORD_STRATEGY_PROGRESS_BONUS = 1_450;
    private static final int STRATEGIC_PLAN_PROGRESS_BONUS = 1_250;
    private static final int LAYOUT_PATTERN_PROGRESS_BONUS = 1_550;
    private static final int HIDDEN_INFORMATION_PROGRESS_BONUS = 1_150;
    private static final int PIECE_ROLE_PROGRESS_BONUS = 1_250;
    private static final int STRATEGIC_EXCHANGE_BONUS = 1_350;
    private static final int HIDDEN_WORST_CASE_PENALTY = 2_400;
    private static final int CANDIDATE_PLAN_BONUS = 1_650;
    private static final int OPPONENT_INTENT_BONUS = 1_500;
    private static final int PIN_PRESSURE_BONUS = 1_250;
    private static final int FORCING_TACTIC_PROGRESS_BONUS = 1_700;
    private static final int ADAPTIVE_TEMPO_BONUS = 1_300;
    private static final int RECORD_STRUCTURE_BONUS = 1_150;
    private static final int HIDDEN_ONE_SHOT_PRESERVE_BONUS = 1_650;
    private static final int DARK_UNCERTAIN_EXCHANGE_EXTRA_PENALTY = 2_200;
    private static final int DARK_RECAPTURE_RISK_PENALTY = 3_400;
    private static final int ROOK_RECAPTURE_PRIORITY_BONUS = 1_250;
    private static final int BAD_TRADE_HIERARCHY_PENALTY = 4_200;
    private static final int UNSOUND_MAJOR_TRADE_CHECK_PENALTY = 5_400;
    private static final int CONTINUATION_TRADE_LOSS_PENALTY = 4_800;
    private static final int MAJOR_SAFETY_PRIORITY_PENALTY = 4_600;
    private static final int ILLUSORY_MAJOR_THREAT_PENALTY = 3_400;
    private static final int POINTLESS_REPOSITION_PENALTY = 2_900;
    private static final int REALIZABLE_PLAN_BONUS = 1_800;
    private static final int NO_FOLLOWUP_DRIFT_PENALTY = 2_700;
    private static final int EARLY_MAJOR_INVASION_BONUS = 2_300;
    private static final int MAJOR_OFFENSIVE_COORDINATION_BONUS = 1_900;
    private static final int SAME_TYPE_COORDINATION_BONUS = 1_450;
    private static final int PALACE_HIDDEN_ACTIVATION_BONUS = 1_650;
    private static final int PHASE_STRATEGY_BONUS = 1_550;
    private static final int COSTLY_CHECK_BLOCK_PENALTY = 5_200;
    private static final int RECOVER_REVEAL_BONUS = 1_850;
    private static final int CONTINUED_REVEAL_BONUS = 1_300;
    private static final int OPPONENT_BAIT_PENALTY = 4_400;
    private static final int BACK_RANK_MAJOR_DRIFT_PENALTY = 2_600;
    private static final int EARLY_NON_DECISIVE_CHECK_PENALTY = 2_400;
    private static final int ROOK_HORIZONTAL_DRIFT_PENALTY = 2_200;
    private static final int HIDDEN_RECAPTURE_UNCERTAIN_TRADE_PENALTY = 2_800;
    private static final int NEAR_KING_MINOR_DRIFT_PENALTY = 1_900;
    private static final int HARASS_CHECK_PENALTY = 3_200;
    private static final int EXCHANGE_SEQUENCE_DEPTH = 8;
    private static final int MATE_SEARCH_DEPTH = 4;
    private static final int MATE_SEARCH_BRANCHING = 18;
    private static final int LATE_MAJOR_PHASE = 58;
    private static final long MIN_SEARCH_MILLIS = 500;
    private static final long MAX_ADAPTIVE_SEARCH_MILLIS = 8_000;
    private static final long NORMAL_POSITION_SEARCH_MILLIS = 4_200;
    private static final long CRITICAL_POSITION_SEARCH_MILLIS = 8_000;
    private static final long STABLE_BEST_SEARCH_MILLIS = 1_800;
    private static final int JIEQI_EARLY_GAME_PHASE = 30;
    private static final int JIEQI_OPENING_MAJOR_PENALTY = 750;
    private static final int JIEQI_OPENING_PAWN_BONUS = 320;

    private final RuleEngine ruleEngine = new RuleEngine();
    private final MoveGenerator moveGenerator = new MoveGenerator(ruleEngine);
    private final XiangqiKnowledge xiangqiKnowledge = new XiangqiKnowledge();
    private final Random random = new Random();
    private final ExperienceMemory experienceMemory;
    private final Move[][] killerMoves = new Move[MAX_DEPTH + QUIESCENCE_DEPTH + EXTENSION_LIMIT + 4][2];
    private final Map<String, Integer> historyScores = new HashMap<>();
    private final Map<String, Integer> heuristicScores = new HashMap<>();
    private final AiConfig config = AiConfig.loadDefault();
    private Map<String, Integer> recordStructureWeights;
    private final Map<String, TranspositionEntry> transpositionTable =
            new LinkedHashMap<String, TranspositionEntry>(16_384, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, TranspositionEntry> eldest) {
                    return size() > TRANSPOSITION_LIMIT;
                }
            };
    private final Map<String, Integer> evalCache = new HashMap<>();
    private static final int EVAL_CACHE_LIMIT = 24_000;

    public SearchAi() {
        this(null);
    }

    public SearchAi(ExperienceMemory experienceMemory) {
        this.experienceMemory = experienceMemory;
    }

    public Move chooseMove(Board board, PlayerColor color, long turnStartTime, long thinkMillis) {
        return chooseMove(board, color, turnStartTime, thinkMillis, move -> 0);
    }

    public Move chooseMove(
            Board board,
            PlayerColor color,
            long turnStartTime,
            long thinkMillis,
            ToIntFunction<Move> rootPenalty) {
        if (experienceMemory != null) {
            experienceMemory.reloadIfChanged();
        }
        if (historyScores.size() > HISTORY_LIMIT) {
            historyScores.clear();
        }
        heuristicScores.clear();
        if (evalCache.size() > EVAL_CACHE_LIMIT) {
            evalCache.clear();
        }
        List<Move> actions = orderedMoves(board, color, turnStartTime);
        if (actions.isEmpty()) {
            return null;
        }
        actions = avoidForcedTacticalLosses(actions, rootPenalty);
        Move forcedWin = immediateWinningMove(board, actions, color);
        if (forcedWin != null) {
            return forcedWin;
        }
        Move forcedMate = forcedMateMove(board, actions, color);
        if (forcedMate != null) {
            return forcedMate;
        }
        actions = avoidRepeatCheckDrift(board, actions, color, rootPenalty);
        actions = avoidIdleAfterRepeatCheckRisk(board, actions, color, rootPenalty);
        actions = avoidPassiveKingDrift(board, actions, color);
        actions = avoidOwnPalaceBlockade(board, actions, color);
        actions = avoidImmediateMajorBlunders(board, actions, color);
        actions = avoidUnsafeLowValueMajorCaptures(board, actions, color);
        actions = avoidBadHierarchyTrades(board, actions, color);
        actions = avoidUnsoundMajorTradeChecks(board, actions, color);
        actions = avoidDarkUncertainRecaptures(board, actions, color);
        actions = preferMajorSafetyResponse(board, actions, color);
        actions = avoidPointlessRepositions(board, actions, color);
        actions = preferPalaceUnblock(board, actions, color);
        actions = avoidObviousGifts(board, actions, color);
        actions = avoidLateMajorDrift(board, actions, color);
        actions = avoidCostlyCheckBlocks(board, actions, color);
        Move urgentHomeDefense = urgentHomeDefenseMove(board, actions, color);
        Move urgentKingDefense = urgentKingDefenseMove(board, actions, color);
        Move forcedMateDefense = urgentForcedMateDefenseMove(board, actions, color);
        if (urgentKingDefense != null) {
            return urgentKingDefense;
        }
        if (forcedMateDefense != null) {
            return forcedMateDefense;
        }
        if (urgentHomeDefense != null) {
            return urgentHomeDefense;
        }
        Move urgentCapture = urgentMaterialCaptureMove(board, actions, color);
        if (urgentCapture != null) {
            return urgentCapture;
        }
        Move urgentThreatenedPiece = urgentThreatenedPieceMove(board, actions, color);
        if (urgentThreatenedPiece != null) {
            return urgentThreatenedPiece;
        }
        actions = preferHomeInvasionResponse(board, actions, color);
        Move usefulReveal = usefulRevealMove(board, actions, color);
        if (usefulReveal != null) {
            return usefulReveal;
        }

        long startedAt = System.currentTimeMillis();
        long budgetMillis = adaptiveThinkMillis(board, actions, color, thinkMillis);
        long deadline = startedAt + budgetMillis;
        boolean criticalPosition = criticalThinkPosition(board, color);
        boolean needsDeep = needsDeepEndgameSearch(board, color);
        int effectiveMaxDepth = (criticalPosition || needsDeep) ? MAX_DEPTH : NORMAL_MAX_DEPTH;
        Move best = actions.get(0);
        int bestScore = Integer.MIN_VALUE;
        int bestMargin = 0;
        Move previousBest = null;
        int stableBestDepths = 0;
        for (int depth = 1; depth <= effectiveMaxDepth; depth++) {
            SearchResult result = searchRoot(board, color, actions, depth, deadline, rootPenalty, previousBest);
            if (result.timedOut()) {
                break;
            }
            if (result.move() != null) {
                stableBestDepths = sameMove(previousBest, result.move()) ? stableBestDepths + 1 : 0;
                best = result.move();
                bestScore = result.score();
                bestMargin = result.margin();
                previousBest = result.move();
            }
            if (Math.abs(bestScore) >= WIN_SCORE / 2) {
                break;
            }
            long elapsed = System.currentTimeMillis() - startedAt;
            if (!criticalPosition && depth >= 3 && bestMargin >= confidentMargin(board, color) + 2_000
                    && elapsed >= config.longValue("search.minMillis", MIN_SEARCH_MILLIS)) {
                break;
            }
            if (depth >= 4 && bestMargin >= confidentMargin(board, color)
                    && elapsed >= confidentStopMillis(board, color, criticalPosition)) {
                break;
            }
            if (!criticalPosition && depth >= 3 && stableBestDepths >= 1
                    && elapsed >= config.longValue("search.stableBestMillis", STABLE_BEST_SEARCH_MILLIS)) {
                break;
            }
            if (!criticalPosition && depth >= 3 && elapsed >= config.longValue("search.minMillis", MIN_SEARCH_MILLIS)) {
                break;
            }
            if (criticalPosition && depth >= 4 && stableBestDepths >= 1
                    && elapsed >= config.longValue("search.stableBestMillis", STABLE_BEST_SEARCH_MILLIS) + 2_500) {
                break;
            }
            if (!criticalPosition && !needsDeep && depth >= effectiveMaxDepth) {
                break;
            }
        }
        return best;
    }

    private int confidentMargin(Board board, PlayerColor color) {
        int margin = 2_400;
        if (ruleEngine.isInCheck(board, color)) {
            margin += 1_600;
        }
        if (opponentPlanThreatScore(board, color) >= 4_200 || legalKingMoveCount(board, color) <= 1) {
            margin += 1_400;
        }
        if (hiddenPhase(board) >= 60) {
            margin += 700;
        }
        return margin;
    }

    private long confidentStopMillis(Board board, PlayerColor color, boolean criticalPosition) {
        long stableMillis = config.longValue("search.stableBestMillis", STABLE_BEST_SEARCH_MILLIS);
        if (!criticalPosition) {
            return Math.max(config.longValue("search.minMillis", MIN_SEARCH_MILLIS), stableMillis / 2);
        }
        long extra = opponentPlanThreatScore(board, color) >= 4_200 ? 1_800 : 900;
        return stableMillis + extra;
    }

    private long adaptiveThinkMillis(Board board, List<Move> actions, PlayerColor color, long requestedMillis) {
        int hiddenPhase = hiddenPhase(board);
        int actionCount = Math.min(MAX_BRANCHING, Math.max(1, actions.size()));
        long minMillis = config.longValue("search.minMillis", MIN_SEARCH_MILLIS);
        boolean criticalPosition = criticalThinkPosition(board, color);
        long configuredMaxMillis = config.longValue("search.maxAdaptiveMillis", MAX_ADAPTIVE_SEARCH_MILLIS);
        long normalMaxMillis = config.longValue("search.normalMaxMillis", NORMAL_POSITION_SEARCH_MILLIS);
        long criticalMaxMillis = config.longValue("search.criticalMaxMillis", CRITICAL_POSITION_SEARCH_MILLIS);
        long maxMillis = Math.min(configuredMaxMillis, criticalPosition ? criticalMaxMillis : normalMaxMillis);
        long budget = minMillis
                + hiddenPhase * 24L
                + actionCount * 42L;
        if (ruleEngine.isInCheck(board, color)) {
            budget += 1_400;
        }
        if (criticalPosition) {
            budget += 2_200;
        }
        int planDanger = opponentPlanThreatScore(board, color);
        if (planDanger >= 5_000) {
            budget += 1_300;
        } else if (planDanger < 1_800 && hiddenPhase < 35 && actions.size() > 22) {
            budget -= 1_200;
        }
        budget = Math.max(minMillis, Math.min(maxMillis, budget));
        return Math.max(100, Math.min(Math.max(100, requestedMillis), budget));
    }

    private boolean criticalThinkPosition(Board board, PlayerColor color) {
        return ruleEngine.isInCheck(board, color)
                || opponentPlanThreatScore(board, color) >= 4_200
                || legalKingMoveCount(board, color) <= 1
                || kingDangerScore(board, color) >= 3_600
                || immediateThreatScore(board, color.opponent()) >= 4_800;
    }

    private boolean needsDeepEndgameSearch(Board board, PlayerColor color) {
        return visiblePhase(board) >= LATE_MAJOR_PHASE
                || invadingPieceScore(board, color) >= 1_800
                || opponentPlanThreatScore(board, color) >= 4_200
                || legalKingMoveCount(board, color) <= 1
                || ruleEngine.isInCheck(board, color);
    }

    private SearchResult searchRoot(
            Board board,
            PlayerColor color,
            List<Move> actions,
            int depth,
            long deadline,
            ToIntFunction<Move> rootPenalty,
            Move previousBest) {
        List<Move> rootActions = orderRootMoves(board, color, actions, previousBest, rootPenalty);
        Move best = null;
        int bestScore = Integer.MIN_VALUE;
        int secondBestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE + 1;
        int beta = Integer.MAX_VALUE;
        int searched = 0;
        for (Move move : rootActions) {
            if (System.currentTimeMillis() >= deadline) {
                int margin = secondBestScore == Integer.MIN_VALUE ? Integer.MAX_VALUE : bestScore - secondBestScore;
                return SearchResult.timeout(best, bestScore, margin);
            }
            int score = searchChildScore(board, move, color.opponent(), color, depth, alpha, beta,
                    deadline, 0, EXTENSION_LIMIT) + tacticalMoveScore(board, move, color);
            Board next = applyForSearch(board, move);
            score += urgentDefenseScore(board, next, move, color);
            score -= immediateThreatScore(next, color.opponent()) / 2;
            score -= exposedMovePenalty(board, next, move, color);
            score -= idleInvadingMajorPenalty(board, next, move, color);
            score -= backRankMajorDriftPenalty(board, next, move, color);
            score -= rookHorizontalDriftPenalty(board, next, move, color);
            score -= earlyNonDecisiveCheckPenalty(board, next, move, color);
            score -= costlyCheckBlockPenalty(board, next, move, color);
            score -= opponentBaitPenalty(board, next, move, color);
            score -= kingRecaptureMajorPenalty(board, next, move, color);
            score -= lateMajorNonProgressPenalty(board, next, move, color);
            score -= lowValueCheckPenalty(board, next, move, color);
            score -= harassCheckPenalty(board, next, move, color);
            score -= aimlessHarassPenalty(board, next, move, color);
            score -= unsoundCheckSacrificePenalty(board, next, move, color);
            score -= ownPalaceBlockPenalty(board, next, move, color);
            score += palaceUnblockProgressScore(board, next, move, color);
            score -= nextMoveMajorLossPenalty(board, next, move, color);
            score -= badTradeHierarchyPenalty(board, next, move, color);
            score -= unsoundMajorTradeCheckPenalty(board, next, move, color);
            score -= continuationTradeLossPenalty(board, next, move, color);
            score -= hiddenRecaptureUncertainTradePenalty(board, next, move, color);
            score -= darkUncertainRecaptureRiskPenalty(board, next, move, color);
            score -= majorSafetyNeglectPenalty(board, next, move, color);
            score -= illusoryMajorThreatPenalty(board, next, move, color);
            score -= pointlessRepositionPenalty(board, next, move, color);
            score -= nearKingMinorDriftPenalty(board, next, move, color);
            if (depth >= 2 && searched <= 10) {
                score -= noFollowupDriftPenalty(board, next, move, color);
            }
            score -= uncertainRecapturePenalty(board, next, move, color);
            score -= hiddenWorstCaseRiskPenalty(board, next, move, color);
            score -= hiddenOneShotWastePenalty(board, next, move, color);
            score -= uncompensatedSacrificePenalty(board, next, move, color);
            score -= obviousGiftPenalty(board, next, move, color);
            score -= escapedCheckExposurePenalty(board, next, move, color);
            score += kingAttackProgressScore(board, next, move, color);
            score += endgameMateNetProgressScore(board, next, move, color);
            score += activeBreakthroughScore(board, next, move, color);
            score += activePieceActivationScore(board, next, move, color);
            score += earlyMajorInvasionScore(board, next, move, color);
            score += majorOffensiveCoordinationProgressScore(board, next, move, color);
            score += sameTypeCoordinationProgressScore(board, next, move, color);
            score += palaceHiddenActivationScore(board, next, move, color);
            score += recoveryRevealProgressScore(board, next, move, color);
            score += phaseStrategyProgressScore(board, next, move, color);
            if (depth >= 2 && searched <= 10) {
                score += realizablePlanProgressScore(board, next, move, color);
            }
            score += flyingKingCoordinationProgressScore(board, next, move, color);
            score += coordinatedAttackProgressScore(board, next, move, color);
            score += formationProgressScore(board, next, move, color);
            score += recordStrategyProgressScore(board, next, move, color);
            score += strategicPlanProgressScore(board, next, move, color);
            score += layoutPatternProgressScore(board, next, move, color);
            score += forcingTacticProgressScore(board, next, move, color);
            score += candidatePlanProgressScore(board, next, move, color);
            score += opponentIntentDisruptionScore(board, next, move, color);
            score += adaptiveTempoScore(board, next, move, color);
            score += pieceRoleProgressScore(board, next, move, color);
            score += strategicExchangeScore(board, next, move, color);
            score += hiddenInformationProgressScore(board, next, move, color);
            score += pawnTrapProgressScore(board, next, move, color);
            score += pawnConstrictionProgressScore(board, next, move, color);
            score += cannonUnlockProgressScore(board, next, move, color);
            score += cannonScreenProgressScore(board, next, move, color);
            score += knightKillShapeProgressScore(board, next, move, color);
            score += advancedPawnKingThreatScore(next, color) - advancedPawnKingThreatScore(board, color);
            score += experienceBonus(board, move) / 2;
            score += configuredRootAdjustment(board, next, move, color);
            score -= rootPenalty.applyAsInt(move);
            if (score > bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                best = move;
            } else {
                secondBestScore = Math.max(secondBestScore, score);
                if (shouldExploreNearBest(board, color, score, bestScore, depth)) {
                    best = move;
                }
            }
            alpha = Math.max(alpha, score);
            searched++;
            if (depth >= 3 && searched >= MAX_BRANCHING) {
                break;
            }
        }
        int margin = secondBestScore == Integer.MIN_VALUE ? Integer.MAX_VALUE : bestScore - secondBestScore;
        return SearchResult.ok(best, bestScore, margin);
    }

    private int configuredRootAdjustment(Board before, Board after, Move move, PlayerColor color) {
        int score = 0;
        score += configDelta("weight.attack", kingAttackProgressScore(before, after, move, color)
                + endgameMateNetProgressScore(before, after, move, color)
                + coordinatedAttackProgressScore(before, after, move, color));
        score += configDelta("weight.defense", urgentDefenseScore(before, after, move, color));
        score += configDelta("weight.reveal", hiddenInformationProgressScore(before, after, move, color));
        score += configDelta("weight.exchange", strategicExchangeScore(before, after, move, color));
        score += configDelta("weight.layout", layoutPatternProgressScore(before, after, move, color)
                + candidatePlanProgressScore(before, after, move, color));
        score += configDelta("weight.intent", opponentIntentDisruptionScore(before, after, move, color)
                + adaptiveTempoScore(before, after, move, color));
        score += configDelta("weight.hiddenRisk", -hiddenWorstCaseRiskPenalty(before, after, move, color));
        return score;
    }

    private int configDelta(String key, int score) {
        return score * (config.intValue(key, 100) - 100) / 100;
    }

    private List<Move> orderRootMoves(
            Board board,
            PlayerColor color,
            List<Move> actions,
            Move previousBest,
            ToIntFunction<Move> rootPenalty) {
        TranspositionEntry cached = transpositionTable.get(transpositionKey(board, color, color));
        String hashMove = cached == null ? null : cached.bestMoveNotation();
        return actions.stream()
                .sorted(Comparator.comparingInt((Move move) -> rootOrderScore(
                        board, move, color, previousBest, hashMove, rootPenalty)).reversed())
                .toList();
    }

    private int rootOrderScore(
            Board board,
            Move move,
            PlayerColor color,
            Move previousBest,
            String hashMove,
            ToIntFunction<Move> rootPenalty) {
        int score = moveOrderScore(board, move, color, 0, hashMove);
        if (sameMove(previousBest, move)) {
            score += 140_000;
        }
        if (hashMove != null && hashMove.equals(move.notation())) {
            score += 120_000;
        }
        score -= rootPenalty.applyAsInt(move);
        return score;
    }

    private List<Move> avoidForcedTacticalLosses(List<Move> actions, ToIntFunction<Move> rootPenalty) {
        List<Move> safer = actions.stream()
                .filter(move -> rootPenalty.applyAsInt(move) < FORCED_TACTICAL_LOSS_PENALTY)
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> avoidRepeatCheckDrift(
            Board board,
            List<Move> actions,
            PlayerColor color,
            ToIntFunction<Move> rootPenalty) {
        List<Move> safer = actions.stream()
                .filter(move -> {
                    int penalty = rootPenalty.applyAsInt(move);
                    if (penalty < 45_000) {
                        return true;
                    }
                    Board next = applyForSearch(board, move);
                    if (!ruleEngine.isInCheck(next, color.opponent())) {
                        return true;
                    }
                    if (!moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    int threatGain = immediateThreatScore(next, color) - immediateThreatScore(board, color);
                    int netGain = endgameMateNetScore(next, color) - endgameMateNetScore(board, color);
                    int materialGain = board.get(move.destination()) == null
                            ? 0
                            : captureValue(board, board.get(move.destination()), move.destination());
                    return threatGain >= 1_500 || netGain >= 1_100 || materialGain >= HIGH_VALUE_PIECE;
                })
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> avoidIdleAfterRepeatCheckRisk(
            Board board,
            List<Move> actions,
            PlayerColor color,
            ToIntFunction<Move> rootPenalty) {
        boolean repeatCheckRisk = actions.stream()
                .anyMatch(move -> rootPenalty.applyAsInt(move) >= 45_000
                        && ruleEngine.isInCheck(applyForSearch(board, move), color.opponent())
                        && moveGenerator.hasCheckEscape(applyForSearch(board, move), color.opponent()));
        if (!repeatCheckRisk) {
            return actions;
        }
        List<Move> purposeful = actions.stream()
                .filter(move -> {
                    if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                        return true;
                    }
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())
                            && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    Piece captured = board.get(move.destination());
                    if (captured != null && captured.color() == color.opponent()) {
                        return true;
                    }
                    Piece mover = board.get(move.source());
                    if (mover != null && checkingPieceForwardPressureProgress(board, next, move, color) >= 420) {
                        return true;
                    }
                    int planGain = candidatePlanScore(next, color) - candidatePlanScore(board, color)
                            + strategicPlanPotential(next, color) - strategicPlanPotential(board, color);
                    int defenseRelief = kingDangerScore(board, color) - kingDangerScore(next, color)
                            + opponentPlanThreatScore(board, color) - opponentPlanThreatScore(next, color);
                    return planGain >= 900 || defenseRelief >= 1_200
                            || palaceUnblockProgressScore(board, next, move, color) >= PALACE_UNBLOCK_BONUS / 2;
                })
                .toList();
        return purposeful.isEmpty() ? actions : purposeful;
    }

    private List<Move> avoidBadHierarchyTrades(Board board, List<Move> actions, PlayerColor color) {
        if (ruleEngine.isInCheck(board, color)) {
            return actions;
        }
        List<Move> safer = actions.stream()
                .filter(move -> {
                    if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                        return true;
                    }
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())
                            && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    int penalty = urgentTradePenalty(board, next, move, color, false);
                    return penalty < BAD_TRADE_HIERARCHY_PENALTY;
                })
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> preferMajorSafetyResponse(Board board, List<Move> actions, PlayerColor color) {
        if (ruleEngine.isInCheck(board, color)) {
            return actions;
        }
        int currentRisk = threatenedMajorSafetyScore(board, color);
        if (currentRisk < MAJOR_SAFETY_PRIORITY_PENALTY / 2) {
            return actions;
        }
        List<Move> responses = actions.stream()
                .filter(move -> {
                    if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                        return true;
                    }
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())
                            && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    Piece captured = board.get(move.destination());
                    if (captured != null && captured.color() == color.opponent()
                            && captureValue(board, captured, move.destination()) >= HIGH_VALUE_PIECE) {
                        return true;
                    }
                    int riskDrop = currentRisk - threatenedMajorSafetyScore(next, color);
                    if (riskDrop >= Math.min(900, currentRisk / 2)) {
                        return true;
                    }
                    Piece mover = board.get(move.source());
                    return mover != null
                            && isMajorPieceForSafety(mover)
                            && legalAttackersValue(board, move.source(), color.opponent()) > 0
                            && legalAttackersValue(next, move.destination(), color.opponent()) == 0;
                })
                .toList();
        return responses.isEmpty() ? actions : responses;
    }

    private List<Move> avoidPointlessRepositions(Board board, List<Move> actions, PlayerColor color) {
        if (ruleEngine.isInCheck(board, color)) {
            return actions;
        }
        List<Move> purposeful = actions.stream()
                .filter(move -> {
                    if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                        return true;
                    }
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())
                            && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    Piece captured = board.get(move.destination());
                    if (captured != null) {
                        return true;
                    }
                    if (palaceUnblockProgressScore(board, next, move, color) >= PALACE_UNBLOCK_BONUS / 2) {
                        return true;
                    }
                    return pointlessRepositionPenalty(board, next, move, color) < POINTLESS_REPOSITION_PENALTY + 1_000;
                })
                .toList();
        return purposeful.isEmpty() ? actions : purposeful;
    }

    private List<Move> preferPalaceUnblock(Board board, List<Move> actions, PlayerColor color) {
        if (ruleEngine.isInCheck(board, color) || palaceBlockadeRisk(board, color) < PALACE_UNBLOCK_BONUS) {
            return actions;
        }
        List<Move> unblocks = actions.stream()
                .filter(move -> {
                    if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                        return true;
                    }
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())
                            && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    Piece captured = board.get(move.destination());
                    if (captured != null && captured.color() == color.opponent()
                            && captureValue(board, captured, move.destination()) >= HIGH_VALUE_PIECE) {
                        return true;
                    }
                    return palaceUnblockProgressScore(board, next, move, color) >= PALACE_UNBLOCK_BONUS / 2;
                })
                .toList();
        return unblocks.isEmpty() ? actions : unblocks;
    }

    private List<Move> avoidPassiveKingDrift(Board board, List<Move> actions, PlayerColor color) {
        if (ruleEngine.isInCheck(board, color)) {
            return actions;
        }
        List<Move> purposeful = actions.stream()
                .filter(move -> {
                    Piece mover = board.get(move.source());
                    if (mover == null || !mover.visible() || mover.type() != PieceType.KING) {
                        return true;
                    }
                    if (board.get(move.destination()) != null) {
                        return true;
                    }
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())) {
                        return true;
                    }
                    int threatRelief = immediateThreatScore(board, color.opponent())
                            - immediateThreatScore(next, color.opponent());
                    int pressureRelief = kingPressure(board, color.opponent())
                            - kingPressure(next, color.opponent());
                    int layoutGain = layoutPatternScore(next, color) - layoutPatternScore(board, color);
                    return threatRelief >= 1_500 || pressureRelief >= 450 || layoutGain >= 650;
                })
                .toList();
        return purposeful.isEmpty() ? actions : purposeful;
    }

    private List<Move> avoidImmediateMajorBlunders(Board board, List<Move> actions, PlayerColor color) {
        List<Move> safer = actions.stream()
                .filter(move -> {
                    Board next = applyForSearch(board, move);
                    return kingRecaptureMajorPenalty(board, next, move, color) < KING_RECAPTURE_MAJOR_PENALTY;
                })
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> avoidUnsafeLowValueMajorCaptures(Board board, List<Move> actions, PlayerColor color) {
        List<Move> safer = actions.stream()
                .filter(move -> !unsafeMajorForLowValueCapture(board, applyForSearch(board, move), move, color))
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> avoidUnsoundMajorTradeChecks(Board board, List<Move> actions, PlayerColor color) {
        List<Move> safer = actions.stream()
                .filter(move -> unsoundMajorTradeCheckPenalty(board, applyForSearch(board, move), move, color)
                        < UNSOUND_MAJOR_TRADE_CHECK_PENALTY)
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> avoidDarkUncertainRecaptures(Board board, List<Move> actions, PlayerColor color) {
        List<Move> safer = actions.stream()
                .filter(move -> darkUncertainRecaptureRiskPenalty(board, applyForSearch(board, move), move, color)
                        < DARK_RECAPTURE_RISK_PENALTY)
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> avoidObviousGifts(Board board, List<Move> actions, PlayerColor color) {
        if (ruleEngine.isInCheck(board, color)) {
            return actions;
        }
        List<Move> safer = actions.stream()
                .filter(move -> {
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())
                            && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    return obviousGiftPenalty(board, next, move, color) < OBVIOUS_GIFT_PENALTY * 2;
                })
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> avoidOwnPalaceBlockade(Board board, List<Move> actions, PlayerColor color) {
        if (ruleEngine.isInCheck(board, color)) {
            return actions;
        }
        List<Move> safer = actions.stream()
                .filter(move -> {
                    Board next = applyForSearch(board, move);
                    return ownPalaceBlockPenalty(board, next, move, color) < OWN_PALACE_BLOCK_PENALTY
                            + KING_ESCAPE_LOSS_PENALTY;
                })
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private List<Move> preferHomeInvasionResponse(Board board, List<Move> actions, PlayerColor color) {
        int currentDanger = invadingPieceScore(board, color);
        if (currentDanger < 1_800) {
            return actions;
        }
        List<Move> responses = actions.stream()
                .filter(move -> {
                    Board next = applyForSearch(board, move);
                    if (ruleEngine.isInCheck(next, color.opponent())
                            && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                        return true;
                    }
                    if (unsafeMajorForLowValueCapture(board, next, move, color)) {
                        return false;
                    }
                    int relief = currentDanger - invadingPieceScore(next, color);
                    int attackGain = immediateThreatScore(next, color) - immediateThreatScore(board, color);
                    if (relief >= 650) {
                        return true;
                    }
                    return attackGain >= 2_500;
                })
                .toList();
        return responses.isEmpty() ? actions : responses;
    }

    private boolean unsafeMajorForLowValueCapture(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece captured = before.get(move.destination());
        if (mover == null || captured == null || captured.color() != color.opponent()) {
            return false;
        }
        int moverValue = pieceSearchValue(before, mover, move.source());
        int capturedValue = captureValue(before, captured, move.destination());
        return moverValue >= HIGH_VALUE_PIECE
                && capturedValue < HIGH_VALUE_PIECE
                && exposedMovePenalty(before, after, move, color) > capturedValue
                && homeInvaderDanger(before, move.destination(), color) < 2_600;
    }

    private List<Move> avoidLateMajorDrift(Board board, List<Move> actions, PlayerColor color) {
        if (strategicPhase(board, color) < LATE_MAJOR_PHASE) {
            return actions;
        }
        List<Move> purposeful = actions.stream()
                .filter(move -> {
                    Piece mover = board.get(move.source());
                    if (mover != null
                            && (knownType(mover) == PieceType.ROOK
                            || knownType(mover) == PieceType.CANNON
                            || knownType(mover) == PieceType.KNIGHT)) {
                        Board next = applyForSearch(board, move);
                        if (attackersValue(board, move.source(), color.opponent()) > 0
                                && attackersValue(next, move.destination(), color.opponent()) == 0) {
                            return true;
                        }
                    }
                    Board next = applyForSearch(board, move);
                    return lateMajorNonProgressPenalty(board, next, move, color) == 0;
                })
                .toList();
        return purposeful.isEmpty() ? actions : purposeful;
    }

    private List<Move> avoidCostlyCheckBlocks(Board board, List<Move> actions, PlayerColor color) {
        if (!ruleEngine.isInCheck(board, color)) {
            return actions;
        }
        int bestLoss = bestCheckDefenseLoss(board, actions, color);
        if (bestLoss == Integer.MAX_VALUE) {
            return actions;
        }
        List<Move> safer = actions.stream()
                .filter(move -> {
                    if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                        return false;
                    }
                    Board next = applyForSearch(board, move);
                    int loss = netCheckDefenseLoss(board, next, move, color);
                    Piece mover = board.get(move.source());
                    PieceType type = mover == null ? PieceType.PAWN : knownType(mover);
                    int margin = (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT)
                            ? value(PieceType.PAWN)
                            : value(PieceType.KNIGHT);
                    return loss <= bestLoss + margin;
                })
                .toList();
        return safer.isEmpty() ? actions : safer;
    }

    private boolean shouldExploreNearBest(Board board, PlayerColor color, int score, int bestScore, int depth) {
        if (bestScore == Integer.MIN_VALUE || score < bestScore - STOCHASTIC_MARGIN) {
            return false;
        }
        if (depth >= 3 || strategicPhase(board, color) >= LATE_MAJOR_PHASE || needsDeepEndgameSearch(board, color)) {
            return false;
        }
        int chance = depth <= 2 ? 5 : 12;
        return random.nextInt(100) < chance;
    }

    private int searchChildScore(
            Board board,
            Move move,
            PlayerColor nextSide,
            PlayerColor aiColor,
            int depth,
            int alpha,
            int beta,
            long deadline,
            int ply,
            int extensionsRemaining) {
        Piece mover = board.get(move.source());
        if (mover != null && !mover.visible() && shouldUseHiddenExpectation(depth, ply)) {
            return hiddenExpectationScore(board, move, nextSide, aiColor, depth, alpha, beta,
                    deadline, ply, extensionsRemaining, mover.color());
        }
        Board next = applyForSearch(board, move);
        return minimaxAfterMove(next, nextSide, aiColor, depth, alpha, beta, deadline, ply, extensionsRemaining);
    }

    private boolean shouldUseHiddenExpectation(int depth, int ply) {
        return ply <= HIDDEN_EXPECT_PLY_LIMIT && depth >= 2;
    }

    private int hiddenExpectationScore(
            Board board,
            Move move,
            PlayerColor nextSide,
            PlayerColor aiColor,
            int depth,
            int alpha,
            int beta,
            long deadline,
            int ply,
            int extensionsRemaining,
            PlayerColor hiddenColor) {
        Map<PieceType, Integer> counts = remainingHiddenTypeCounts(board, hiddenColor);
        // Optimized: only do full search for top-value types, approximate the rest
        int total = 0;
        int weightedScore = 0;
        PieceType[] priorityTypes = {PieceType.ROOK, PieceType.CANNON, PieceType.KNIGHT,
                PieceType.PAWN, PieceType.GUARD, PieceType.BISHOP};
        int fullSearchCount = 0;
        for (PieceType pieceType : priorityTypes) {
            int count = counts.getOrDefault(pieceType, 0);
            if (count <= 0) {
                continue;
            }
            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            int score;
            if (fullSearchCount < 3) {
                // Full minimax search for top 3 most valuable types
                Board next = applyForSearch(board, move, pieceType);
                score = minimaxAfterMove(next, nextSide, aiColor, depth, alpha, beta,
                        deadline, ply, extensionsRemaining);
                fullSearchCount++;
            } else {
                // Fast approximation for remaining types
                score = evaluate(applyForSearch(board, move, pieceType), aiColor);
            }
            total += count;
            weightedScore += score * count;
        }
        if (total == 0) {
            Board next = applyForSearch(board, move);
            return minimaxAfterMove(next, nextSide, aiColor, depth, alpha, beta,
                    deadline, ply, extensionsRemaining);
        }
        return weightedScore / total;
    }

    private int minimaxAfterMove(
            Board next,
            PlayerColor nextSide,
            PlayerColor aiColor,
            int depth,
            int alpha,
            int beta,
            long deadline,
            int ply,
            int extensionsRemaining) {
        int nextDepth = nextDepth(depth, extensionsRemaining, next, nextSide);
        int nextExtensions = nextDepth == depth ? extensionsRemaining - 1 : extensionsRemaining;
        return minimax(next, nextSide, aiColor, nextDepth, alpha, beta,
                deadline, ply + 1, nextExtensions, true);
    }

    private int minimax(
            Board board,
            PlayerColor sideToMove,
            PlayerColor aiColor,
            int depth,
            int alpha,
            int beta,
            long deadline,
            int ply,
            int extensionsRemaining,
            boolean nullAllowed) {
        if (System.currentTimeMillis() >= deadline) {
            return evaluate(board, aiColor);
        }
        if (kingMissing(board, aiColor)) {
            return -WIN_SCORE;
        }
        if (kingMissing(board, aiColor.opponent())) {
            return WIN_SCORE;
        }
        if (depth == 0) {
            return quiescence(board, sideToMove, aiColor, alpha, beta, deadline, QUIESCENCE_DEPTH, ply);
        }

        int alphaOriginal = alpha;
        int betaOriginal = beta;
        int depthBudget = depth + extensionsRemaining;
        String key = transpositionKey(board, sideToMove, aiColor);
        TranspositionEntry cached = transpositionTable.get(key);
        String hashMove = cached == null ? null : cached.bestMoveNotation();
        if (cached != null && cached.depthBudget() >= depthBudget) {
            if (cached.bound() == Bound.EXACT) {
                return cached.score();
            }
            if (cached.bound() == Bound.LOWER) {
                alpha = Math.max(alpha, cached.score());
            } else {
                beta = Math.min(beta, cached.score());
            }
            if (alpha >= beta) {
                return cached.score();
            }
        }

        if (canUseNullMove(board, sideToMove, depth, extensionsRemaining, nullAllowed)) {
            int reducedDepth = Math.max(0, depth - NULL_MOVE_REDUCTION - 1);
            int nullScore = minimax(board, sideToMove.opponent(), aiColor, reducedDepth, alpha, beta,
                    deadline, ply + 1, 0, false);
            if ((sideToMove == aiColor && nullScore >= beta)
                    || (sideToMove != aiColor && nullScore <= alpha)) {
                rememberTransposition(key, depthBudget, nullScore, alphaOriginal, betaOriginal, deadline, null);
                return nullScore;
            }
        }

        if (hashMove == null && depth >= IID_MIN_DEPTH) {
            Move iidMove = internalIterativeMove(board, sideToMove, aiColor, depth, alpha, beta,
                    deadline, ply, extensionsRemaining);
            if (iidMove != null) {
                hashMove = iidMove.notation();
            }
        }

        boolean inCheck = ruleEngine.isInCheck(board, sideToMove);
        int futilityMargin = depth <= 1 && !inCheck ? FUTILITY_MARGINS[Math.min(depth, FUTILITY_MARGINS.length - 1)] : Integer.MAX_VALUE;

        List<Move> actions = orderedMoves(board, sideToMove, System.currentTimeMillis(), ply, hashMove);
        int maxBranching = criticalThinkPosition(board, aiColor) || inCheck ? MAX_BRANCHING : NON_CRITICAL_MAX_BRANCHING;
        if (depth >= 2 && actions.size() > maxBranching) {
            actions = actions.subList(0, maxBranching);
        }
        if (actions.isEmpty()) {
            int score = inCheck ? WIN_SCORE / 2 : 35_000;
            return sideToMove == aiColor ? -score : score;
        }

        if (sideToMove == aiColor) {
            int best = Integer.MIN_VALUE + 1;
            Move bestMove = null;
            boolean first = true;
            int moveIndex = 0;
            int staticEval = depth <= 1 && !inCheck ? evaluate(board, aiColor) : 0;
            for (Move move : actions) {
                if (depth <= 1 && !inCheck && moveIndex >= 2
                        && staticEval + futilityMargin <= alpha
                        && board.get(move.destination()) == null
                        && !ruleEngine.isInCheck(applyForSearch(board, move), sideToMove.opponent())) {
                    moveIndex++;
                    continue;
                }
                boolean reduced = useLateMoveReduction(board, move, sideToMove, depth, moveIndex, ply);
                int searchDepth = reduced ? depth - 1 : depth;
                int score;
                if (first || alpha >= beta - 1) {
                    score = searchChildScore(board, move, sideToMove.opponent(), aiColor, searchDepth, alpha, beta,
                            deadline, ply, extensionsRemaining);
                } else {
                    int nullBeta = Math.min(beta, alpha + 1);
                    score = searchChildScore(board, move, sideToMove.opponent(), aiColor, searchDepth, alpha, nullBeta,
                            deadline, ply, extensionsRemaining);
                    if (score > alpha && score < beta) {
                        score = searchChildScore(board, move, sideToMove.opponent(), aiColor, searchDepth, alpha, beta,
                                deadline, ply, extensionsRemaining);
                    }
                }
                if (reduced && score > alpha) {
                    score = searchChildScore(board, move, sideToMove.opponent(), aiColor, depth, alpha, beta,
                            deadline, ply, extensionsRemaining);
                }
                first = false;
                moveIndex++;
                if (score > best) {
                    best = score;
                    bestMove = move;
                }
                alpha = Math.max(alpha, score);
                if (alpha >= beta) {
                    rememberCutoff(board, sideToMove, move, depth, ply);
                    break;
                }
            }
            rememberTransposition(key, depthBudget, best, alphaOriginal, betaOriginal, deadline, bestMove);
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            Move bestMove = null;
            boolean first = true;
            int moveIndex = 0;
            int staticEval = depth <= 1 && !inCheck ? evaluate(board, aiColor) : 0;
            for (Move move : actions) {
                if (depth <= 1 && !inCheck && moveIndex >= 2
                        && staticEval - futilityMargin >= beta
                        && board.get(move.destination()) == null
                        && !ruleEngine.isInCheck(applyForSearch(board, move), sideToMove.opponent())) {
                    moveIndex++;
                    continue;
                }
                boolean reduced = useLateMoveReduction(board, move, sideToMove, depth, moveIndex, ply);
                int searchDepth = reduced ? depth - 1 : depth;
                int score;
                if (first || alpha >= beta - 1) {
                    score = searchChildScore(board, move, sideToMove.opponent(), aiColor, searchDepth, alpha, beta,
                            deadline, ply, extensionsRemaining);
                } else {
                    int nullAlpha = Math.max(alpha, beta - 1);
                    score = searchChildScore(board, move, sideToMove.opponent(), aiColor, searchDepth, nullAlpha, beta,
                            deadline, ply, extensionsRemaining);
                    if (score > alpha && score < beta) {
                        score = searchChildScore(board, move, sideToMove.opponent(), aiColor, searchDepth, alpha, beta,
                                deadline, ply, extensionsRemaining);
                    }
                }
                if (reduced && score < beta) {
                    score = searchChildScore(board, move, sideToMove.opponent(), aiColor, depth, alpha, beta,
                            deadline, ply, extensionsRemaining);
                }
                first = false;
                moveIndex++;
                if (score < best) {
                    best = score;
                    bestMove = move;
                }
                beta = Math.min(beta, score);
                if (alpha >= beta) {
                    rememberCutoff(board, sideToMove, move, depth, ply);
                    break;
                }
            }
            rememberTransposition(key, depthBudget, best, alphaOriginal, betaOriginal, deadline, bestMove);
            return best;
        }
    }

    private List<Move> orderedMoves(Board board, PlayerColor color, long turnStartTime) {
        return orderedMoves(board, color, turnStartTime, 0, null);
    }

    private List<Move> orderedMoves(Board board, PlayerColor color, long turnStartTime, int ply) {
        return orderedMoves(board, color, turnStartTime, ply, null);
    }

    private List<Move> orderedMoves(
            Board board,
            PlayerColor color,
            long turnStartTime,
            int ply,
            String hashMove) {
        List<Move> actions = moveGenerator.generateActions(board, color, turnStartTime);
        List<Move> safeActions = actions.stream()
                .filter(move -> ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color))
                .toList();
        if (ruleEngine.isInCheck(board, color)) {
            actions = safeActions;
        } else if (!safeActions.isEmpty()) {
            actions = safeActions;
        }
        return actions.stream()
                .sorted(Comparator.comparingInt((Move move) -> moveOrderScore(board, move, color, ply, hashMove))
                        .reversed())
                .toList();
    }

    private int moveOrderScore(Board board, Move move, PlayerColor color, int ply, String hashMove) {
        Piece mover = board.get(move.source());
        Piece captured = board.get(move.destination());
        int score = 0;
        if (captured != null) {
            score += 10_000 + captureValue(board, captured, move.destination()) * 6
                    - pieceSearchValue(board, mover, move.source()) / 3;
        }
        if (hashMove != null && hashMove.equals(move.notation())) {
            score += 90_000;
        }
        Board next = applyForSearch(board, move);
        if (ruleEngine.isInCheck(next, color.opponent())) {
            score += 4_200;
        }
        if (ruleEngine.isInCheck(next, color)) {
            score -= 8_000;
        }
        if (mover != null && !mover.visible()) {
            score += revealMoveScore(board, move, color, next);
        }
        score -= passiveKingMovePenalty(board, move, color, next);
        if (captured == null) {
            if (isKillerMove(ply, move)) {
                score += 2_200;
            }
            score += historyScores.getOrDefault(historyKey(color, move), 0);
        }
        score += tacticalMoveScore(board, move, color);
        score -= badExchangePenalty(board, next, move, color);
        score += urgentDefenseScore(board, next, move, color);
        score += rootedExchangeScore(board, next, move, color);
        score -= exposedMovePenalty(board, next, move, color);
        score -= idleInvadingMajorPenalty(board, next, move, color) / 2;
        score -= backRankMajorDriftPenalty(board, next, move, color) / 2;
        score -= rookHorizontalDriftPenalty(board, next, move, color) / 2;
        score -= earlyNonDecisiveCheckPenalty(board, next, move, color) / 2;
        score -= costlyCheckBlockPenalty(board, next, move, color) / 2;
        score -= opponentBaitPenalty(board, next, move, color) / 2;
        score -= kingRecaptureMajorPenalty(board, next, move, color);
        score -= lateMajorNonProgressPenalty(board, next, move, color) / 2;
        score -= lowValueCheckPenalty(board, next, move, color) / 2;
        score -= harassCheckPenalty(board, next, move, color) / 2;
        score -= aimlessHarassPenalty(board, next, move, color) / 2;
        score -= unsoundCheckSacrificePenalty(board, next, move, color) / 2;
        score -= ownPalaceBlockPenalty(board, next, move, color) / 2;
        score += palaceUnblockProgressScore(board, next, move, color) / 2;
        score -= nextMoveMajorLossPenalty(board, next, move, color) / 2;
        score -= badTradeHierarchyPenalty(board, next, move, color) / 2;
        score -= unsoundMajorTradeCheckPenalty(board, next, move, color) / 2;
        score -= continuationTradeLossPenalty(board, next, move, color) / 2;
        score -= hiddenRecaptureUncertainTradePenalty(board, next, move, color) / 2;
        score -= darkUncertainRecaptureRiskPenalty(board, next, move, color) / 2;
        score -= majorSafetyNeglectPenalty(board, next, move, color) / 2;
        score -= illusoryMajorThreatPenalty(board, next, move, color) / 2;
        score -= pointlessRepositionPenalty(board, next, move, color) / 2;
        score -= nearKingMinorDriftPenalty(board, next, move, color) / 2;
        score -= uncertainRecapturePenalty(board, next, move, color) / 2;
        score -= hiddenWorstCaseRiskPenalty(board, next, move, color) / 2;
        score -= hiddenOneShotWastePenalty(board, next, move, color) / 2;
        score -= uncompensatedSacrificePenalty(board, next, move, color) / 2;
        score -= escapedCheckExposurePenalty(board, next, move, color) / 2;
        score += kingAttackProgressScore(board, next, move, color) / 2;
        score += endgameMateNetProgressScore(board, next, move, color) / 2;
        score += activeBreakthroughScore(board, next, move, color) / 2;
        score += activePieceActivationScore(board, next, move, color) / 2;
        score += earlyMajorInvasionScore(board, next, move, color) / 2;
        score += majorOffensiveCoordinationProgressScore(board, next, move, color) / 2;
        score += sameTypeCoordinationProgressScore(board, next, move, color) / 2;
        score += palaceHiddenActivationScore(board, next, move, color) / 2;
        score += recoveryRevealProgressScore(board, next, move, color) / 2;
        score += phaseStrategyProgressScore(board, next, move, color) / 2;
        score += flyingKingCoordinationProgressScore(board, next, move, color) / 2;
        score += coordinatedAttackProgressScore(board, next, move, color) / 2;
        score += formationProgressScore(board, next, move, color) / 2;
        score += recordStrategyProgressScore(board, next, move, color) / 2;
        score += strategicPlanProgressScore(board, next, move, color) / 2;
        score += layoutPatternProgressScore(board, next, move, color) / 2;
        score += forcingTacticProgressScore(board, next, move, color) / 2;
        score += candidatePlanProgressScore(board, next, move, color) / 2;
        score += opponentIntentDisruptionScore(board, next, move, color) / 2;
        score += adaptiveTempoScore(board, next, move, color) / 2;
        score += pieceRoleProgressScore(board, next, move, color) / 2;
        score += strategicExchangeScore(board, next, move, color) / 2;
        score += hiddenInformationProgressScore(board, next, move, color) / 2;
        score += pawnTrapProgressScore(board, next, move, color) / 2;
        score += pawnConstrictionProgressScore(board, next, move, color) / 2;
        score += cannonUnlockProgressScore(board, next, move, color) / 2;
        score += cannonScreenProgressScore(board, next, move, color) / 2;
        score += knightKillShapeProgressScore(board, next, move, color) / 2;
        score += (advancedPawnKingThreatScore(next, color) - advancedPawnKingThreatScore(board, color)) / 2;
        score += jieqiShapePressure(next, color) / 2;
        score += immediateThreatScore(next, color) / FOLLOWUP_THREAT_BONUS;
        score -= immediateThreatScore(next, color.opponent()) / REPLY_THREAT_PENALTY;
        score += experienceBonus(board, move);
        score += jieqiOpeningGuidance(board, move, color);
        return score;
    }

    // Jieqi opening strategy: avoid prematurely revealing Rook/Cannon position hidden pieces,
    // prioritize revealing pawn-position pieces first.
    private int jieqiOpeningGuidance(Board board, Move move, PlayerColor color) {
        int visiblePhase = visiblePhase(board);
        if (visiblePhase > JIEQI_EARLY_GAME_PHASE) {
            return 0;
        }
        Piece mover = board.get(move.source());
        if (mover == null || mover.visible()) {
            return 0;
        }
        PieceType moveType = mover.hiddenMoveType();
        int earlyGameWeight = (JIEQI_EARLY_GAME_PHASE - visiblePhase) * 100 / JIEQI_EARLY_GAME_PHASE;
        return switch (moveType) {
            case ROOK, CANNON -> -(JIEQI_OPENING_MAJOR_PENALTY - 120) * earlyGameWeight / 100;
            case PAWN -> JIEQI_OPENING_PAWN_BONUS * earlyGameWeight / 100;
            case BISHOP -> JIEQI_OPENING_PAWN_BONUS * earlyGameWeight / 200;
            default -> 0;
        };
    }

    private int revealMoveScore(Board before, Move move, PlayerColor color, Board after) {
        Piece moved = after.get(move.destination());
        if (moved == null) {
            return 0;
        }
        int hiddenPhase = hiddenPhase(before);
        int score = REVEAL_BONUS + hiddenPhase * 13 + expectedHiddenValue(before, color) / 2;
        score += remainingHighValueHiddenCount(before, color) * hiddenPhase * 3;
        int attackers = attackersValue(after, move.destination(), color.opponent());
        int defenders = defendersValue(after, move.destination(), color);
        if (attackers == 0) {
            score += 360 + hiddenPhase * 4;
        } else if (defenders == 0) {
            score -= pieceSearchValue(after, moved, move.destination()) * (120 - hiddenPhase) / 240;
        } else {
            score += Math.min(defenders, pieceSearchValue(after, moved, move.destination())) / 6;
        }
        if (ruleEngine.isInCheck(after, color.opponent())) {
            score += 650;
        }
        return score;
    }

    private int passiveKingMovePenalty(Board before, Move move, PlayerColor color, Board after) {
        Piece mover = before.get(move.source());
        if (mover == null || !mover.visible() || mover.type() != PieceType.KING) {
            return 0;
        }
        if (ruleEngine.isInCheck(before, color)
                || before.get(move.destination()) != null
                || ruleEngine.isInCheck(after, color.opponent())) {
            return 0;
        }
        int beforePressure = kingPressure(before, color.opponent());
        int afterPressure = kingPressure(after, color.opponent());
        int relief = Math.max(0, beforePressure - afterPressure);
        int layoutGain = Math.max(0, layoutPatternScore(after, color) - layoutPatternScore(before, color));
        int penalty = Math.max(1_200, PASSIVE_KING_MOVE_PENALTY - relief * 4);
        penalty = Math.max(0, penalty - layoutGain * 3);
        if (idleKingMove(before, after, color)) {
            penalty += layoutGain >= 650 ? IDLE_KING_MOVE_EXTRA_PENALTY / 4 : IDLE_KING_MOVE_EXTRA_PENALTY;
        }
        return penalty;
    }

    private boolean idleKingMove(Board before, Board after, PlayerColor color) {
        int ownThreatBefore = immediateThreatScore(before, color);
        int ownThreatAfter = immediateThreatScore(after, color);
        int enemyThreatBefore = immediateThreatScore(before, color.opponent());
        int enemyThreatAfter = immediateThreatScore(after, color.opponent());
        return ownThreatAfter <= ownThreatBefore + 120
                && enemyThreatAfter >= enemyThreatBefore - 180
                && mobilityDelta(before, after, color) <= 1;
    }

    private int quiescence(
            Board board,
            PlayerColor sideToMove,
            PlayerColor aiColor,
            int alpha,
            int beta,
            long deadline,
            int quietDepth,
            int ply) {
        if (System.currentTimeMillis() >= deadline || quietDepth == 0) {
            return evaluate(board, aiColor);
        }
        if (kingMissing(board, aiColor)) {
            return -WIN_SCORE;
        }
        if (kingMissing(board, aiColor.opponent())) {
            return WIN_SCORE;
        }

        boolean inCheck = ruleEngine.isInCheck(board, sideToMove);
        int standPat = evaluate(board, aiColor);
        if (!inCheck) {
            if (sideToMove == aiColor) {
                if (standPat >= beta) {
                    return beta;
                }
                alpha = Math.max(alpha, standPat);
            } else {
                if (standPat <= alpha) {
                    return alpha;
                }
                beta = Math.min(beta, standPat);
            }
        }

        List<Move> actions = inCheck
                ? orderedMoves(board, sideToMove, System.currentTimeMillis(), ply)
                : tacticalMoves(board, sideToMove, ply);
        int qBranching = inCheck || criticalThinkPosition(board, aiColor) ? QUIESCENCE_BRANCHING : NON_CRITICAL_QUIESCENCE_BRANCHING;
        if (actions.size() > qBranching) {
            actions = actions.subList(0, qBranching);
        }
        if (actions.isEmpty()) {
            if (inCheck) {
                int score = WIN_SCORE / 2;
                return sideToMove == aiColor ? -score : score;
            }
            return standPat;
        }

        if (sideToMove == aiColor) {
            int best = inCheck ? Integer.MIN_VALUE + 1 : standPat;
            for (Move move : actions) {
                Board next = applyForSearch(board, move);
                int score = quiescence(next, sideToMove.opponent(), aiColor, alpha, beta,
                        deadline, quietDepth - 1, ply + 1);
                best = Math.max(best, score);
                alpha = Math.max(alpha, score);
                if (alpha >= beta) {
                    break;
                }
            }
            return best;
        }

        int best = inCheck ? Integer.MAX_VALUE : standPat;
        for (Move move : actions) {
            Board next = applyForSearch(board, move);
            int score = quiescence(next, sideToMove.opponent(), aiColor, alpha, beta,
                    deadline, quietDepth - 1, ply + 1);
            best = Math.min(best, score);
            beta = Math.min(beta, score);
            if (alpha >= beta) {
                break;
            }
        }
        return best;
    }

    private List<Move> tacticalMoves(Board board, PlayerColor color, int ply) {
        return orderedMoves(board, color, System.currentTimeMillis(), ply).stream()
                .filter(move -> board.get(move.destination()) != null
                        || ruleEngine.isInCheck(applyForSearch(board, move), color.opponent())
                        || isUsefulRevealMove(board, move, color))
                .toList();
    }

    private boolean isUsefulRevealMove(Board board, Move move, PlayerColor color) {
        Piece mover = board.get(move.source());
        if (mover == null || mover.visible() || hiddenPhase(board) < REVEAL_QUIESCENCE_PHASE) {
            return false;
        }
        Board next = applyForSearch(board, move);
        return revealMoveScore(board, move, color, next) >= REVEAL_BONUS + 600;
    }

    private boolean useLateMoveReduction(
            Board board,
            Move move,
            PlayerColor color,
            int depth,
            int moveIndex,
            int ply) {
        if (depth < LMR_MIN_DEPTH || moveIndex < LMR_MOVE_INDEX || ruleEngine.isInCheck(board, color)) {
            return false;
        }
        Piece mover = board.get(move.source());
        if (mover != null && !mover.visible()) {
            return false;
        }
        if (hiddenPhase(board) >= 45) {
            return false;
        }
        if (board.get(move.destination()) != null || isKillerMove(ply, move)) {
            return false;
        }
        Board next = applyForSearch(board, move);
        return !ruleEngine.isInCheck(next, color.opponent());
    }

    private int experienceBonus(Board board, Move move) {
        if (experienceMemory == null) {
            return 0;
        }
        String key = knownPositionKey(board);
        int exact = experienceMemory.bonus(key, move);
        if (exact != 0) {
            return scaleExperienceBonus(board, key, exact);
        }
        Move mirroredMove = mirrorMove(move);
        if (mirroredMove == null) {
            return 0;
        }
        String mirroredKey = mirroredKnownPositionKey(board);
        int mirrored = experienceMemory.bonus(mirroredKey, mirroredMove);
        return scaleExperienceBonus(board, mirroredKey, mirrored);
    }

    private int scaleExperienceBonus(Board board, String key, int bonus) {
        int visiblePhase = visiblePhase(board);
        if (key.contains(":HIDDEN:")) {
            return bonus * (45 + visiblePhase / 2) / 100;
        }
        return bonus * Math.max(20, visiblePhase) / 100;
    }

    private boolean canUseNullMove(
            Board board,
            PlayerColor sideToMove,
            int depth,
            int extensionsRemaining,
            boolean nullAllowed) {
        return nullAllowed
                && depth >= NULL_MOVE_MIN_DEPTH
                && extensionsRemaining == EXTENSION_LIMIT
                && visiblePhase(board) >= 55
                && !ruleEngine.isInCheck(board, sideToMove)
                && visibleMaterial(board, sideToMove) >= NULL_MOVE_MIN_MATERIAL
                && visibleNonKingPieces(board, sideToMove) >= 3;
    }

    private Move internalIterativeMove(
            Board board,
            PlayerColor sideToMove,
            PlayerColor aiColor,
            int depth,
            int alpha,
            int beta,
            long deadline,
            int ply,
            int extensionsRemaining) {
        int shallowDepth = Math.max(1, depth / 2);
        List<Move> candidates = orderedMoves(board, sideToMove, System.currentTimeMillis(), ply, null);
        if (candidates.size() > MAX_BRANCHING / 2) {
            candidates = candidates.subList(0, MAX_BRANCHING / 2);
        }
        Move bestMove = null;
        int best = sideToMove == aiColor ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE;
        for (Move move : candidates) {
            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            int score = searchChildScore(board, move, sideToMove.opponent(), aiColor, shallowDepth,
                    alpha, beta, deadline, ply, Math.min(extensionsRemaining, 1));
            if (sideToMove == aiColor ? score > best : score < best) {
                best = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int visibleMaterial(Board board, PlayerColor color) {
        int material = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece != null && piece.color() == color && piece.visible() && piece.type() != PieceType.KING) {
                material += value(piece.type());
            }
        }
        return material;
    }

    private int visibleNonKingPieces(Board board, PlayerColor color) {
        int count = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece != null && piece.color() == color && piece.visible() && piece.type() != PieceType.KING) {
                count++;
            }
        }
        return count;
    }

    private int nextDepth(int depth, int extensionsRemaining, Board next, PlayerColor nextSide) {
        int nextDepth = depth - 1;
        if (extensionsRemaining > 0 && ruleEngine.isInCheck(next, nextSide)) {
            return depth;
        }
        return nextDepth;
    }

    private String transpositionKey(Board board, PlayerColor sideToMove, PlayerColor aiColor) {
        return sideToMove.name() + "|" + aiColor.name() + "|" + knownPositionKey(board);
    }

    private void rememberTransposition(
            String key,
            int depthBudget,
            int score,
            int alphaOriginal,
            int betaOriginal,
            long deadline,
            Move bestMove) {
        if (System.currentTimeMillis() >= deadline) {
            return;
        }
        Bound bound = Bound.EXACT;
        if (score <= alphaOriginal) {
            bound = Bound.UPPER;
        } else if (score >= betaOriginal) {
            bound = Bound.LOWER;
        }
        transpositionTable.put(key, new TranspositionEntry(
                depthBudget,
                score,
                bound,
                bestMove == null ? null : bestMove.notation()));
    }

    private void rememberCutoff(Board board, PlayerColor color, Move move, int depth, int ply) {
        if (board.get(move.destination()) != null) {
            return;
        }
        int index = Math.min(ply, killerMoves.length - 1);
        if (!sameMove(killerMoves[index][0], move)) {
            killerMoves[index][1] = killerMoves[index][0];
            killerMoves[index][0] = move;
        }
        String key = historyKey(color, move);
        int bonus = Math.min(1_200, depth * depth * 24);
        historyScores.merge(key, bonus, (oldScore, add) -> Math.min(4_000, oldScore + add));
    }

    private boolean isKillerMove(int ply, Move move) {
        int index = Math.min(ply, killerMoves.length - 1);
        return sameMove(killerMoves[index][0], move) || sameMove(killerMoves[index][1], move);
    }

    private String historyKey(PlayerColor color, Move move) {
        return color.name() + "|" + move.notation();
    }

    private boolean sameMove(Move first, Move second) {
        return first != null && second != null
                && first.source().equals(second.source())
                && first.destination().equals(second.destination())
                && first.flipOnly() == second.flipOnly();
    }

    private int tacticalMoveScore(Board board, Move move, PlayerColor color) {
        Piece mover = board.get(move.source());
        if (mover == null) {
            return 0;
        }
        Piece captured = board.get(move.destination());
        Board next = applyForSearch(board, move);
        int score = 0;
        if (captured != null) {
            PieceType capturedKnownType = knownType(captured);
            score += captureValue(board, captured, move.destination()) * 3;
            if (capturedKnownType == PieceType.KING) {
                score += WIN_SCORE / 2;
            }
        }
        score += checkPressureBonus(board, next, move, color);
        if (ruleEngine.isInCheck(next, color)) {
            score -= SELF_CHECK_PENALTY;
        }
        score += pawnAttackProgressScore(board, next, move, color);

        Piece moved = next.get(move.destination());
        int movedValue = moved == null ? 0 : pieceSearchValue(next, moved, move.destination());
        int attackers = attackersValue(next, move.destination(), color.opponent());
        int defenders = defendersValue(next, move.destination(), color);
        if (attackers > 0) {
            score -= exchangeSequenceGain(next, move.destination(), color.opponent()) + badExchangePenalty(board, next, move, color);
            score += Math.min(defenders, movedValue) / 4;
        } else if (defenders > 0) {
            score += Math.min(defenders, movedValue) / 8;
        }
        score += mobilityDelta(board, next, color) * 3;
        return score;
    }

    private int checkPressureBonus(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(after, color.opponent())) {
            return 0;
        }
        if (!moveGenerator.hasCheckEscape(after, color.opponent())) {
            return WIN_SCORE / 4;
        }
        Piece moved = after.get(move.destination());
        if (moved == null || knownType(moved) == PieceType.KING) {
            return CHECK_BONUS / 6;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = 0;
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            capturedValue = captureValue(before, captured, move.destination());
        }
        int recaptureLoss = directReplyCaptureLoss(after, move, color, movedValue);
        if (recaptureLoss > Math.max(120, capturedValue / 2)) {
            return 0;
        }
        int planGain = Math.max(0, endgameMateNetScore(after, color) - endgameMateNetScore(before, color))
                + Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color)) / 2;
        if (planGain >= 1_200) {
            return CHECK_BONUS / 2;
        }
        return CHECK_BONUS / 5;
    }

    private int badExchangePenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int opponentExchangeGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        if (opponentExchangeGain == 0) {
            return 0;
        }

        int materialLoss = Math.max(movedValue - capturedValue, opponentExchangeGain - capturedValue);
        if (materialLoss <= 0) {
            return movedValue >= HIGH_VALUE_PIECE ? movedValue / 5 : 0;
        }

        int defenders = defendersValue(after, move.destination(), color);
        int defenderDiscount = defenders == 0 ? 0 : Math.min(defenders, movedValue) / 5;
        int visibilityMultiplier = moved.visible() ? BAD_EXCHANGE_PENALTY : 2;
        int highValueExtra = movedValue >= HIGH_VALUE_PIECE && capturedValue <= value(PieceType.PAWN)
                ? movedValue * 2
                : 0;
        return Math.max(0, materialLoss * visibilityMultiplier + highValueExtra - defenderDiscount);
    }

    private int rootedExchangeScore(Board before, Board after, Move move, PlayerColor color) {
        Piece moved = after.get(move.destination());
        if (moved == null) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int attackerGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        int defender = defendersValue(after, move.destination(), color);

        if (attackerGain == 0) {
            return defender > 0 ? Math.min(ROOTED_EXCHANGE_BONUS, movedValue / 6) : 0;
        }

        int recaptureRisk = Math.max(0, attackerGain - capturedValue);
        if (defender == 0) {
            return -Math.max(80, recaptureRisk + movedValue / 4);
        }
        if (recaptureRisk <= Math.max(60, capturedValue / 3)) {
            return Math.min(ROOTED_EXCHANGE_BONUS, Math.max(40, capturedValue / 5));
        }
        return -Math.max(40, recaptureRisk / 2);
    }

    private int strategicExchangeScore(Board before, Board after, Move move, PlayerColor color) {
        Piece captured = before.get(move.destination());
        Piece mover = before.get(move.source());
        if (captured == null || captured.color() != color.opponent() || mover == null
                || knownType(captured) == PieceType.KING || knownType(mover) == PieceType.KING) {
            return 0;
        }
        int capturedValue = captureValue(before, captured, move.destination());
        int moverValue = pieceSearchValue(before, mover, move.source());
        int opponentGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        int net = capturedValue - Math.max(0, opponentGain - forcedRecaptureValue(after, move.destination(), color));
        int score = 0;
        if (capturedValue >= HIGH_VALUE_PIECE && net >= -120) {
            score += 720;
        }
        if (materialBalance(before, color) > 900 && capturedValue >= HIGH_VALUE_PIECE && moverValue <= capturedValue + 180) {
            score += 680;
        }
        if (opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color) >= 800) {
            score += 900;
        }
        if (kingDangerScore(before, color) - kingDangerScore(after, color) >= 900) {
            score += 760;
        }
        if (knownType(captured) == PieceType.ROOK) {
            if (knownType(mover) == PieceType.ROOK && exchangeSequenceGain(after, move.destination(), color.opponent()) <= capturedValue) {
                score += ROOK_RECAPTURE_PRIORITY_BONUS;
            } else if (safeOwnRookCanCapture(before, move.destination(), color)) {
                score -= ROOK_RECAPTURE_PRIORITY_BONUS;
                if (mover.visible() || knownType(mover) != PieceType.ROOK) {
                    score -= 520;
                }
            }
        }
        if (capturedValue < HIGH_VALUE_PIECE && moverValue >= HIGH_VALUE_PIECE
                && opponentGain > capturedValue && homeInvaderDanger(before, move.destination(), color) < 2_200) {
            score -= 1_200;
        }
        if (!captured.visible()) {
            score -= Math.max(0, HIDDEN_WORST_CASE_PENALTY - hiddenInvaderInformationDanger(before, move.destination(), color));
        }
        return Math.max(-1_600, Math.min(STRATEGIC_EXCHANGE_BONUS, score));
    }

    private int opponentBaitPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        if (movedValue < value(PieceType.KNIGHT)) {
            return 0;
        }
        int opponentGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        int directLoss = directReplyCaptureLoss(after, move, color, movedValue);
        int expectedLoss = Math.max(opponentGain, directLoss);
        if (expectedLoss <= 0) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int capturedValue = captured == null || captured.color() != color.opponent()
                ? 0
                : captureValue(before, captured, move.destination());
        int compensation = capturedValue
                + forcedRecaptureValue(after, move.destination(), color)
                + Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color)) / 4
                + Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color)) / 3
                + baitTrapScore(after, move.destination(), color) / 2;
        if (compensation >= expectedLoss) {
            return 0;
        }
        int netLoss = expectedLoss - compensation;
        int penalty = OPPONENT_BAIT_PENALTY + netLoss * 4;
        if (capturedValue == 0) {
            penalty += movedValue;
        }
        if (capturedValue < value(PieceType.KNIGHT)) {
            penalty += movedValue / 2;
        }
        if (!mover.visible() || (captured != null && !captured.visible())) {
            penalty += 700;
        }
        return Math.min(25_000, penalty);
    }

    private boolean safeOwnRookCanCapture(Board board, Position target, PlayerColor color) {
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) != PieceType.ROOK) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, source, target, color)) {
                continue;
            }
            Board after = applyForSearch(board, Move.move(source, target, 0));
            Piece captured = board.get(target);
            int capturedValue = captured == null ? 0 : captureValue(board, captured, target);
            if (exchangeSequenceGain(after, target, color.opponent()) <= capturedValue + 180) {
                return true;
            }
        }
        return false;
    }

    private int materialBalance(Board board, PlayerColor color) {
        int balance = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || knownType(piece) == PieceType.KING) {
                continue;
            }
            int value = pieceSearchValue(board, piece, position);
            balance += piece.color() == color ? value : -value;
        }
        return balance;
    }

    private int idleInvadingMajorPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || !mover.visible()) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
            return 0;
        }
        if (!isOpponentSide(move.source(), color) || !isOpponentSide(move.destination(), color)) {
            return 0;
        }
        if (before.get(move.destination()) != null) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())) {
            return 0;
        }
        int beforeThreat = immediateThreatScore(before, color);
        int afterThreat = immediateThreatScore(after, color);
        if (afterThreat >= beforeThreat + 900) {
            return 0;
        }
        int pressureGain = jieqiShapePressure(after, color) - jieqiShapePressure(before, color);
        if (pressureGain >= 220) {
            return 0;
        }
        int penalty = switch (type) {
            case ROOK -> 1_900;
            case CANNON -> 1_450;
            case KNIGHT -> 1_100;
            default -> 0;
        };
        int homeRank = color == PlayerColor.RED
                ? Position.HEIGHT - 1 - move.destination().y()
                : move.destination().y();
        if (homeRank <= 2) {
            penalty += 500;
        }
        return penalty;
    }

    private int backRankMajorDriftPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || !mover.visible()) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.ROOK && type != PieceType.CANNON) {
            return 0;
        }
        if (before.get(move.destination()) != null || ruleEngine.isInCheck(before, color)) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int hiddenPhase = hiddenPhase(before);
        if (hiddenPhase < 35 || isOpponentSide(move.destination(), color)) {
            return 0;
        }
        int sourceRank = forwardRank(move.source(), color);
        int destinationRank = forwardRank(move.destination(), color);
        int progress = destinationRank - sourceRank;
        int roleGain = pieceRoleScore(after, color) - pieceRoleScore(before, color);
        int attackGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int defenseRelief = Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color))
                + Math.max(0, opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color));
        int screenGain = type == PieceType.CANNON ? cannonScreenScore(after, color) - cannonScreenScore(before, color) : 0;
        if (progress > 0 || roleGain >= 500 || attackGain >= 700 || defenseRelief >= 900 || screenGain >= 320) {
            return 0;
        }
        int penalty = BACK_RANK_MAJOR_DRIFT_PENALTY + hiddenPhase * (type == PieceType.ROOK ? 18 : 12);
        if (sameRank(move.source(), move.destination())) {
            penalty += type == PieceType.ROOK ? 700 : 420;
        }
        if (standsBehindOwnHiddenPiece(before, move.destination(), color)) {
            penalty += type == PieceType.ROOK ? 1_100 : 620;
        }
        if (openLineMobility(after, move.destination()) <= openLineMobility(before, move.source())) {
            penalty += 520;
        }
        return Math.min(8_000, penalty);
    }

    private int rookHorizontalDriftPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || !mover.visible() || knownType(mover) != PieceType.ROOK) {
            return 0;
        }
        if (before.get(move.destination()) != null || ruleEngine.isInCheck(before, color)) {
            return 0;
        }
        if (!sameRank(move.source(), move.destination()) || isOpponentSide(move.destination(), color)) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int rankGain = forwardRank(move.destination(), color) - forwardRank(move.source(), color);
        int attackGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int roleGain = pieceRoleScore(after, color) - pieceRoleScore(before, color);
        int layoutGain = layoutPatternScore(after, color) - layoutPatternScore(before, color);
        int defenseRelief = Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color))
                + Math.max(0, opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color));
        Position enemyKing = before.findKing(color.opponent());
        boolean usefulLine = enemyKing != null && sameFileOrRank(move.destination(), enemyKing)
                && after.countBetween(move.destination(), enemyKing) <= 1;
        if (rankGain > 0 || attackGain >= 900 || roleGain >= 700 || layoutGain >= 700
                || defenseRelief >= 1_000 || usefulLine) {
            return 0;
        }
        int penalty = ROOK_HORIZONTAL_DRIFT_PENALTY;
        if (forwardRank(move.destination(), color) <= 2) {
            penalty += 800;
        }
        if (majorHiddenTargets(after, move.destination(), color) == 0) {
            penalty += 600;
        }
        if (openLineMobility(after, move.destination()) <= openLineMobility(before, move.source())) {
            penalty += 520;
        }
        return Math.min(7_500, penalty);
    }

    private int earlyMajorInvasionScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || !mover.visible()) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.ROOK && type != PieceType.CANNON) {
            return 0;
        }
        int hiddenPhase = hiddenPhase(before);
        if (hiddenPhase < 18 || ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        int score = 0;
        int rankGain = forwardRank(move.destination(), color) - forwardRank(move.source(), color);
        if (rankGain > 0) {
            score += Math.min(1_450, rankGain * (type == PieceType.ROOK ? 430 : 300));
        }
        if (isOpponentSide(move.destination(), color)) {
            score += 900 + hiddenPhase * 10;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            score += captureValue(before, captured, move.destination());
            if (!captured.visible()) {
                score += type == PieceType.ROOK ? 900 : 620;
            }
        }
        int hiddenTargets = majorHiddenTargets(after, move.destination(), color);
        score += Math.min(1_500, hiddenTargets * (type == PieceType.ROOK ? 380 : 260));
        if (openLineMobility(after, move.destination()) > openLineMobility(before, move.source())) {
            score += 420;
        }
        if (type == PieceType.CANNON) {
            score += Math.max(0, cannonScreenScore(after, color) - cannonScreenScore(before, color)) / 2;
            score += cannonUnlockProgressScore(before, after, move, color) / 2;
        }
        score += Math.max(0, pieceRoleScore(after, color) - pieceRoleScore(before, color)) / 2;
        if (legalAttackersValue(after, move.destination(), color.opponent()) > 0
                && defendersValue(after, move.destination(), color) == 0) {
            score -= pieceSearchValue(after, moved, move.destination());
        }
        if (type == PieceType.CANNON) {
            score = score * 7 / 10;
        }
        return Math.max(0, Math.min(EARLY_MAJOR_INVASION_BONUS, score));
    }

    private int majorOffensiveCoordinationProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || !moved.visible() || ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        PieceType type = knownType(moved);
        if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
            return 0;
        }
        Position enemyKing = before.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 6;
        }
        int score = 0;
        int rankGain = forwardRank(move.destination(), color) - forwardRank(move.source(), color);
        if (rankGain > 0) {
            score += rankGain * switch (type) {
                case ROOK -> 360;
                case CANNON -> 270;
                case KNIGHT -> 300;
                default -> 0;
            };
        } else if (rankGain < 0 && !ruleEngine.isInCheck(before, color)) {
            score -= Math.abs(rankGain) * 220;
        }
        if (isOpponentSide(move.destination(), color)) {
            score += type == PieceType.ROOK ? 760 : type == PieceType.CANNON ? 540 : 620;
        }
        int beforeDistance = manhattan(move.source(), enemyKing);
        int afterDistance = manhattan(move.destination(), enemyKing);
        if (afterDistance < beforeDistance) {
            score += (beforeDistance - afterDistance) * 180;
        }
        score += Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color)) / 2;
        score += Math.max(0, pinPressureScore(after, color) - pinPressureScore(before, color)) / 2;
        score += nearbyFriendlySupport(after, move.destination(), color) * 140;
        if (type == PieceType.CANNON) {
            score += Math.max(0, cannonScreenScore(after, color) - cannonScreenScore(before, color)) / 3;
        } else if (type == PieceType.KNIGHT) {
            score += Math.max(0, knightKillShapeScore(after, color) - knightKillShapeScore(before, color)) / 3;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        if (legalAttackersValue(after, move.destination(), color.opponent()) > defendersValue(after, move.destination(), color)
                && captureSwing(before, move, color) < movedValue / 2) {
            score -= movedValue;
        }
        return Math.max(0, Math.min(MAJOR_OFFENSIVE_COORDINATION_BONUS, score));
    }

    private int sameTypeCoordinationProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.KING) {
            return 0;
        }
        int progress = sameTypeCoordinationScore(after, color) - sameTypeCoordinationScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(SAME_TYPE_COORDINATION_BONUS, progress);
    }

    private int sameTypeCoordinationScore(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("sameTypeCoord", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        List<Position> rooks = positionsOfKnownType(board, color, PieceType.ROOK);
        List<Position> cannons = positionsOfKnownType(board, color, PieceType.CANNON);
        List<Position> knights = positionsOfKnownType(board, color, PieceType.KNIGHT);
        int score = 0;
        score += pairPressureScore(board, color, enemyKing, rooks, PieceType.ROOK);
        score += pairPressureScore(board, color, enemyKing, cannons, PieceType.CANNON);
        score += pairPressureScore(board, color, enemyKing, knights, PieceType.KNIGHT);
        score += crossMajorPairScore(board, color, enemyKing, rooks, cannons);
        score += idleDuplicateMajorPenalty(board, color, rooks, PieceType.ROOK);
        score += idleDuplicateMajorPenalty(board, color, cannons, PieceType.CANNON);
        score += idleDuplicateMajorPenalty(board, color, knights, PieceType.KNIGHT);
        int result = Math.max(-3_500, Math.min(6_500, score));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private List<Position> positionsOfKnownType(Board board, PlayerColor color, PieceType type) {
        List<Position> positions = new java.util.ArrayList<>();
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece != null && piece.color() == color && knownType(piece) == type) {
                positions.add(source);
            }
        }
        return positions;
    }

    private int pairPressureScore(
            Board board,
            PlayerColor color,
            Position enemyKing,
            List<Position> positions,
            PieceType type) {
        if (positions.size() < 2) {
            return 0;
        }
        int score = 0;
        for (int i = 0; i < positions.size(); i++) {
            for (int j = i + 1; j < positions.size(); j++) {
                Position first = positions.get(i);
                Position second = positions.get(j);
                int firstRank = forwardRank(first, color);
                int secondRank = forwardRank(second, color);
                int firstDistance = manhattan(first, enemyKing);
                int secondDistance = manhattan(second, enemyKing);
                int spacing = manhattan(first, second);
                int frontBack = Math.abs(firstRank - secondRank);
                if (type == PieceType.KNIGHT) {
                    if (firstDistance <= 5 && secondDistance <= 5 && spacing >= 2 && spacing <= 5) {
                        score += 900;
                    }
                    if (Math.abs(first.x() - second.x()) >= 2 && frontBack <= 3) {
                        score += 520;
                    }
                    score += Math.max(0, 7 - Math.min(firstDistance, secondDistance)) * 90;
                } else {
                    if (sameFileOrRank(first, enemyKing) && sameFileOrRank(second, enemyKing)) {
                        score += type == PieceType.ROOK ? 1_050 : 920;
                    } else if (sameFileOrRank(first, enemyKing) || sameFileOrRank(second, enemyKing)) {
                        score += type == PieceType.ROOK ? 620 : 540;
                    }
                    if (Math.abs(first.x() - second.x()) >= 2 && firstRank >= 3 && secondRank >= 3) {
                        score += 560;
                    }
                    if (isOpponentSide(first, color) || isOpponentSide(second, color)) {
                        score += 360;
                    }
                }
                if (nearbyFriendlySupport(board, first, color) > 0
                        || nearbyFriendlySupport(board, second, color) > 0) {
                    score += 240;
                }
            }
        }
        return score;
    }

    private int crossMajorPairScore(
            Board board,
            PlayerColor color,
            Position enemyKing,
            List<Position> rooks,
            List<Position> cannons) {
        int score = 0;
        for (Position rook : rooks) {
            for (Position cannon : cannons) {
                if (sameFileOrRank(rook, enemyKing) && sameFileOrRank(cannon, enemyKing)) {
                    score += 1_150;
                } else if (sameFileOrRank(rook, enemyKing) || sameFileOrRank(cannon, enemyKing)) {
                    score += 620;
                }
                if (manhattan(rook, cannon) >= 2 && manhattan(rook, cannon) <= 5
                        && (isOpponentSide(rook, color) || isOpponentSide(cannon, color))) {
                    score += 520;
                }
                if (sameFileOrRank(cannon, enemyKing) && board.countBetween(cannon, enemyKing) == 1) {
                    score += 560;
                }
            }
        }
        return score;
    }

    private int idleDuplicateMajorPenalty(Board board, PlayerColor color, List<Position> positions, PieceType type) {
        if (positions.size() < 2) {
            return 0;
        }
        int active = 0;
        int idle = 0;
        Position enemyKing = board.findKing(color.opponent());
        for (Position position : positions) {
            int rank = forwardRank(position, color);
            boolean pressure = enemyKing != null && (manhattan(position, enemyKing) <= 5
                    || sameFileOrRank(position, enemyKing)
                    || boardControlsPalace(board, position, color.opponent()));
            if (rank >= 4 || pressure || openLineMobility(board, position) >= 4) {
                active++;
            } else {
                idle++;
            }
        }
        if (active == 0) {
            return type == PieceType.KNIGHT ? -900 : -1_150;
        }
        return idle > 0 ? -idle * (type == PieceType.KNIGHT ? 360 : 520) : 0;
    }

    private int recoveryRevealProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || mover.visible() || ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        int ownHidden = hiddenPieceCount(before, color);
        if (ownHidden < 3 || remainingHighValueHiddenCount(before, color) <= 0) {
            return 0;
        }
        int material = materialBalance(before, color);
        int activeMajors = activeMajorCount(before, color);
        boolean needsRecovery = material < -value(PieceType.CANNON) || activeMajors == 0
                || visibleMajorCount(before, color) < visibleMajorCount(before, color.opponent());
        if (!needsRecovery) {
            return 0;
        }
        if (kingDangerScore(after, color) > kingDangerScore(before, color) + 650) {
            return 0;
        }
        int attackers = attackersValue(after, move.destination(), color.opponent());
        int defenders = defendersValue(after, move.destination(), color);
        if (attackers > 0 && defenders == 0) {
            return 0;
        }
        int score = RECOVER_REVEAL_BONUS / 2
                + hiddenValueSpread(before, color) / 2
                + remainingHighValueHiddenCount(before, color) * 260;
        if (isForwardReveal(move, color)) {
            score += 360;
        }
        if (isOpponentSide(move.destination(), color)) {
            score += 240;
        }
        score += Math.max(0, hiddenInformationScore(after, color) - hiddenInformationScore(before, color)) / 2;
        return Math.min(RECOVER_REVEAL_BONUS, score);
    }

    private int activeMajorCount(Board board, PlayerColor color) {
        int count = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT) {
                count++;
            }
        }
        return count;
    }

    private int visibleMajorCount(Board board, PlayerColor color) {
        int count = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color || !piece.visible()) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT) {
                count++;
            }
        }
        return count;
    }

    private int visibleTypeCount(Board board, PlayerColor color, PieceType type) {
        int count = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece != null && piece.color() == color && piece.visible() && piece.type() == type) {
                count++;
            }
        }
        return count;
    }

    private int palaceHiddenActivationScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || mover.visible() || !ownPalace(move.source(), color)) {
            return 0;
        }
        Position king = before.findKing(color);
        if (king == null || manhattan(move.source(), king) != 1) {
            return 0;
        }
        if (ruleEngine.isInCheck(before, color) || ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        if (remainingHighValueHiddenCount(before, color) <= 0) {
            return 0;
        }
        int dangerDelta = kingDangerScore(after, color) - kingDangerScore(before, color);
        if (dangerDelta > 450 || opponentPlanThreatScore(after, color) > opponentPlanThreatScore(before, color) + 900) {
            return 0;
        }
        int score = PALACE_HIDDEN_ACTIVATION_BONUS / 2;
        PieceType oneShotType = knownType(mover);
        if (oneShotType == PieceType.ROOK || oneShotType == PieceType.CANNON || oneShotType == PieceType.KNIGHT) {
            score += 560;
        }
        if (!ownPalace(move.destination(), color)) {
            score += 360;
        }
        if (palaceBlockadeRisk(after, color) < palaceBlockadeRisk(before, color)) {
            score += PALACE_UNBLOCK_BONUS / 3;
        }
        int revealGain = hiddenInformationScore(after, color) - hiddenInformationScore(before, color);
        if (revealGain > 0) {
            score += revealGain / 2;
        }
        int attackers = attackersValue(after, move.destination(), color.opponent());
        int defenders = defendersValue(after, move.destination(), color);
        if (attackers > 0 && defenders == 0) {
            score -= likelyHighHiddenValue(before, color, move.source());
        }
        if (attackers == 0) {
            score += 220;
        }
        return Math.max(0, Math.min(PALACE_HIDDEN_ACTIVATION_BONUS, score));
    }

    private int phaseStrategyProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        int phase = strategicPhase(before, color);
        int score = 0;
        Piece captured = before.get(move.destination());
        if (phase < 38) {
            if (captured != null && captured.color() == color.opponent()) {
                score += Math.min(1_000, captureValue(before, captured, move.destination()) * 2);
            }
            score += Math.max(0, earlyMajorInvasionScore(before, after, move, color)) / 2;
            score -= ruleEngine.isInCheck(after, color.opponent())
                    && moveGenerator.hasCheckEscape(after, color.opponent()) ? 420 : 0;
        } else if (phase < LATE_MAJOR_PHASE) {
            score += forcingTacticProgressScore(before, after, move, color);
            score += Math.max(0, pinPressureScore(after, color) - pinPressureScore(before, color)) / 2;
            score += Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color)) / 4;
            if (captured != null && captured.color() == color.opponent()) {
                score += Math.min(720, captureValue(before, captured, move.destination()));
            }
        } else {
            score += Math.max(0, kingAttackProgressScore(before, after, move, color));
            score += Math.max(0, endgameMateNetProgressScore(before, after, move, color));
            score += ruleEngine.isInCheck(after, color.opponent())
                    ? (moveGenerator.hasCheckEscape(after, color.opponent()) ? 260 : PHASE_STRATEGY_BONUS)
                    : 0;
            score += Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color)) / 2;
        }
        int gift = obviousGiftPenalty(before, after, move, color)
                + badTradeHierarchyPenalty(before, after, move, color) / 2;
        if (gift > 0) {
            score -= gift / 4;
        }
        return Math.max(0, Math.min(PHASE_STRATEGY_BONUS, score));
    }

    private int majorHiddenTargets(Board board, Position source, PlayerColor color) {
        int count = 0;
        for (Position target : board.occupiedPositions()) {
            if (target.equals(source)) {
                continue;
            }
            Piece piece = board.get(target);
            if (piece != null && piece.color() == color.opponent() && !piece.visible()
                    && ruleEngine.canMove(board, source, target, color)) {
                count++;
            }
        }
        return count;
    }

    private boolean standsBehindOwnHiddenPiece(Board board, Position rook, PlayerColor color) {
        int dy = color.forwardDirection();
        int frontY = rook.y() + dy;
        if (!Position.isInside(rook.x(), frontY)) {
            return false;
        }
        Position front = new Position(rook.x(), frontY);
        Piece piece = board.get(front);
        return piece != null && piece.color() == color && !piece.visible();
    }

    private boolean sameRank(Position first, Position second) {
        return first.y() == second.y();
    }

    private int kingRecaptureMajorPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || !moved.visible()) {
            return 0;
        }
        PieceType type = knownType(moved);
        if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
            return 0;
        }
        if (before.get(move.destination()) != null) {
            return 0;
        }
        if (!canOpponentKingSafelyCapture(after, move.destination(), color)) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int value = pieceSearchValue(after, moved, move.destination());
        int penalty = KING_RECAPTURE_MAJOR_PENALTY + value * 5;
        if (ruleEngine.isInCheck(after, color.opponent())) {
            penalty += 4_000;
        }
        return penalty;
    }

    private int lateMajorNonProgressPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (strategicPhase(before, color) < LATE_MAJOR_PHASE) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null || !mover.visible()) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
            return 0;
        }
        if (before.get(move.destination()) != null) {
            return 0;
        }
        boolean escapedAttack = attackersValue(before, move.source(), color.opponent()) > 0
                && attackersValue(after, move.destination(), color.opponent()) == 0;
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int ownThreatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        if (ownThreatGain >= 800) {
            return 0;
        }
        int pressureGain = jieqiShapePressure(after, color) - jieqiShapePressure(before, color);
        int kingPressureGain = kingPressure(after, color) - kingPressure(before, color);
        if (pressureGain >= 220 || kingPressureGain >= 520) {
            return 0;
        }
        int opponentThreatGain = immediateThreatScore(after, color.opponent())
                - immediateThreatScore(before, color.opponent());
        int penalty = LATE_MAJOR_NON_PROGRESS_PENALTY;
        if (type == PieceType.ROOK) {
            penalty += 450;
        } else if (type == PieceType.CANNON) {
            penalty += 250;
        }
        if (ruleEngine.isInCheck(after, color.opponent())) {
            penalty /= 2;
        }
        if (opponentThreatGain > 0) {
            penalty += opponentThreatGain / 2;
        }
        if (escapedAttack) {
            penalty /= 2;
        }
        return penalty;
    }

    private int lowValueCheckPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(after, color.opponent())
                || !moveGenerator.hasCheckEscape(after, color.opponent())
                || before.get(move.destination()) != null) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.PAWN) {
            return 0;
        }
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int pressureGain = majorKingAttackScore(after, color) - majorKingAttackScore(before, color);
        if (threatGain >= 900 || pressureGain >= 350) {
            return 0;
        }
        int penalty = LOW_VALUE_CHECK_PENALTY;
        if (threatGain <= 120 && pressureGain <= 120) {
            penalty += NON_DECISIVE_CHECK_PENALTY;
        }
        if (strategicPhase(before, color) >= LATE_MAJOR_PHASE) {
            penalty += 1_100;
        }
        if (attackersValue(after, move.destination(), color.opponent()) > 0) {
            penalty += 1_400;
        }
        if (immediateThreatScore(after, color.opponent()) > immediateThreatScore(before, color.opponent())) {
            penalty += 900;
        }
        penalty += aimlessRepeatCheckPenalty(before, after, move, color);
        return config.scale("weight.repeatCheckPenalty", penalty);
    }

    private int earlyNonDecisiveCheckPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(after, color.opponent())
                || !moveGenerator.hasCheckEscape(after, color.opponent())
                || hiddenPhase(before) < 35) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()
                && captureValue(before, captured, move.destination()) >= HIGH_VALUE_PIECE) {
            return 0;
        }
        int planGain = realizablePlanProgressScore(before, after, move, color)
                + Math.max(0, candidatePlanScore(after, color) - candidatePlanScore(before, color));
        int exchangeGain = strategicExchangeScore(before, after, move, color);
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        if (planGain >= 1_200 || exchangeGain >= 900 || threatGain >= 2_000) {
            return 0;
        }
        int penalty = EARLY_NON_DECISIVE_CHECK_PENALTY;
        Piece moved = after.get(move.destination());
        if (moved != null && legalAttackersValue(after, move.destination(), color.opponent()) > 0) {
            penalty += pieceSearchValue(after, moved, move.destination()) / 2;
        }
        if (before.get(move.destination()) == null) {
            penalty += 600;
        }
        return Math.min(7_500, penalty);
    }

    private int harassCheckPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(after, color.opponent())
                || !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.KING) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            return 0;
        }
        PieceType type = knownType(mover);
        int mateGain = endgameMateNetScore(after, color) - endgameMateNetScore(before, color);
        int attackGain = coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color);
        int pressureGain = kingPressure(after, color) - kingPressure(before, color);
        int forwardPressure = checkingPieceForwardPressureProgress(before, after, move, color);
        int forcingTacticGain = forcingTacticProgressScore(before, after, move, color);
        if (mateGain >= 900 || attackGain >= 620 || pressureGain >= 420
                || forwardPressure >= 700 || forcingTacticGain >= FORCING_TACTIC_PROGRESS_BONUS / 2) {
            return 0;
        }
        int penalty = HARASS_CHECK_PENALTY + aimlessRepeatCheckPenalty(before, after, move, color);
        if (strategicPhase(before, color) >= LATE_MAJOR_PHASE) {
            penalty += 1_400;
        }
        if (type == PieceType.ROOK || type == PieceType.CANNON) {
            penalty += 500;
        } else if (type == PieceType.PAWN || type == PieceType.GUARD || type == PieceType.BISHOP) {
            penalty -= 500;
        }
        if (legalAttackersValue(after, move.destination(), color.opponent()) > 0) {
            Piece moved = after.get(move.destination());
            if (moved != null) {
                penalty += pieceSearchValue(after, moved, move.destination()) / 2;
            }
        }
        return Math.min(11_000, penalty);
    }

    private int checkingPieceForwardPressureProgress(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.KING) {
            return 0;
        }
        Position enemyKing = before.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        PieceType type = knownType(mover);
        int score = 0;
        int rankGain = forwardRank(move.destination(), color) - forwardRank(move.source(), color);
        if (rankGain > 0) {
            score += rankGain * switch (type) {
                case ROOK, CANNON -> 220;
                case KNIGHT -> 240;
                case PAWN -> 260;
                case GUARD, BISHOP -> 160;
                case KING -> 0;
            };
        }
        if (isOpponentSide(move.destination(), color)) {
            score += 420;
        }
        int beforeDistance = manhattan(move.source(), enemyKing);
        int afterDistance = manhattan(move.destination(), enemyKing);
        if (afterDistance < beforeDistance) {
            score += (beforeDistance - afterDistance) * 220;
        }
        if (type == PieceType.KNIGHT) {
            score += Math.max(0, knightKillShapeScore(after, color) - knightKillShapeScore(before, color)) / 2;
        } else if (type == PieceType.CANNON) {
            score += Math.max(0, cannonScreenScore(after, color) - cannonScreenScore(before, color)) / 2;
        } else if (type == PieceType.PAWN || type == PieceType.GUARD || type == PieceType.BISHOP) {
            score += nearKingMinorPressureProgress(before, after, move, color);
        }
        score += Math.max(0, legalKingMoveCount(before, color.opponent()) - legalKingMoveCount(after, color.opponent())) * 520;
        return Math.min(2_200, Math.max(0, score));
    }

    private int aimlessHarassPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (before.get(move.destination()) != null) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null || !mover.visible()) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
            return 0;
        }
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int attackGain = coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color);
        int recordGain = recordStrategyScore(after, color) - recordStrategyScore(before, color);
        int planGain = strategicPlanPotential(after, color) - strategicPlanPotential(before, color);
        if (threatGain >= 700 || attackGain >= 420 || recordGain >= 520 || planGain >= 420) {
            return 0;
        }
        int penalty = AIMLESS_HARASS_PENALTY;
        if (ruleEngine.isInCheck(after, color.opponent()) && moveGenerator.hasCheckEscape(after, color.opponent())) {
            penalty += NON_DECISIVE_CHECK_PENALTY + aimlessRepeatCheckPenalty(before, after, move, color);
        }
        if (strategicPhase(before, color) >= LATE_MAJOR_PHASE) {
            penalty += 1_100;
        }
        if (attackersValue(after, move.destination(), color.opponent()) > 0) {
            penalty += 1_250;
        }
        if (sameFileOrRank(move.source(), move.destination())
                && manhattan(move.source(), move.destination()) <= 2
                && !isOpponentSide(move.destination(), color)) {
            penalty += 480;
        }
        return penalty;
    }

    private int aimlessRepeatCheckPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(after, color.opponent())
                || !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int pressureGain = coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color);
        int materialGain = captureSwing(before, move, color);
        if (threatGain >= 500 || pressureGain >= 260 || materialGain >= HIGH_VALUE_PIECE / 2) {
            return 0;
        }
        int penalty = AIMLESS_REPEAT_CHECK_EXTRA_PENALTY;
        if (strategicPhase(before, color) >= LATE_MAJOR_PHASE) {
            penalty += 2_400;
        }
        Piece moved = after.get(move.destination());
        if (moved != null && attackersValue(after, move.destination(), color.opponent()) > 0) {
            penalty += pieceSearchValue(after, moved, move.destination()) / 2;
        }
        return penalty;
    }

    private int captureSwing(Board board, Move move, PlayerColor color) {
        Piece captured = board.get(move.destination());
        if (captured == null || captured.color() != color.opponent()) {
            return 0;
        }
        Piece mover = board.get(move.source());
        int moverValue = mover == null ? 0 : pieceSearchValue(board, mover, move.source());
        return captureValue(board, captured, move.destination()) - moverValue;
    }

    private int ownPalaceBlockPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.KING || !ownPalace(move.destination(), color)) {
            return 0;
        }
        if (ruleEngine.isInCheck(before, color)) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            return 0;
        }
        Position king = before.findKing(color);
        if (king == null) {
            return 0;
        }

        int beforeEscapes = legalKingMoveCount(before, color);
        int afterEscapes = legalKingMoveCount(after, color);
        int escapeLoss = Math.max(0, beforeEscapes - afterEscapes);
        int occupancyGain = Math.max(0, ownPalaceOccupancy(after, color) - ownPalaceOccupancy(before, color));
        boolean adjacentToKing = manhattan(move.destination(), king) == 1;
        boolean movedIntoPalace = !ownPalace(move.source(), color);

        int penalty = 0;
        if (escapeLoss > 0) {
            penalty += KING_ESCAPE_LOSS_PENALTY * escapeLoss;
        }
        if (adjacentToKing) {
            penalty += OWN_PALACE_BLOCK_PENALTY;
        } else if (movedIntoPalace || occupancyGain > 0) {
            penalty += OWN_PALACE_BLOCK_PENALTY / 2;
        }
        if (!mover.visible()) {
            penalty += 900;
        }
        int pressureRelief = Math.max(0, kingPressure(before, color) - kingPressure(after, color));
        int threatRelief = Math.max(0, immediateThreatScore(before, color.opponent())
                - immediateThreatScore(after, color.opponent()));
        penalty -= pressureRelief * 2 + threatRelief / 2;
        return Math.max(0, penalty);
    }

    private int palaceUnblockProgressScore(Board before, Board after, Move move, PlayerColor color) {
        if (ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        int beforeRisk = palaceBlockadeRisk(before, color);
        if (beforeRisk <= 0) {
            return 0;
        }
        int afterRisk = palaceBlockadeRisk(after, color);
        int relief = beforeRisk - afterRisk;
        if (relief <= 0) {
            return 0;
        }
        Piece mover = before.get(move.source());
        int score = relief;
        if (mover != null && ownPalace(move.source(), color) && !ownPalace(move.destination(), color)) {
            score += PALACE_UNBLOCK_BONUS / 2;
        }
        int escapeGain = legalKingMoveCount(after, color) - legalKingMoveCount(before, color);
        if (escapeGain > 0) {
            score += escapeGain * KING_ESCAPE_LOSS_PENALTY;
        }
        int dangerRelief = Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color));
        score += dangerRelief / 2;
        return Math.min(PALACE_UNBLOCK_BONUS * 3, score);
    }

    private int palaceBlockadeRisk(Board board, PlayerColor color) {
        Position king = board.findKing(color);
        if (king == null) {
            return WIN_SCORE / 3;
        }
        int score = Math.max(0, 3 - legalKingMoveCount(board, color)) * KING_ESCAPE_LOSS_PENALTY;
        int blockers = 0;
        for (int y = color == PlayerColor.RED ? 0 : 7; y <= (color == PlayerColor.RED ? 2 : 9); y++) {
            for (int x = 3; x <= 5; x++) {
                Position position = new Position(x, y);
                if (position.equals(king)) {
                    continue;
                }
                Piece piece = board.get(position);
                if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                    continue;
                }
                PieceType type = knownType(piece);
                int defenders = defendersValue(board, position, color);
                int attackers = legalAttackersValue(board, position, color.opponent());
                int block = manhattan(position, king) == 1 ? OWN_PALACE_BLOCK_PENALTY : OWN_PALACE_BLOCK_PENALTY / 2;
                if (type == PieceType.GUARD && defenders > 0 && attackers == 0) {
                    block /= 3;
                } else if (type != PieceType.GUARD) {
                    block += 1_100;
                }
                if (attackers > 0) {
                    block += 900;
                }
                blockers++;
                score += block;
            }
        }
        if (blockers >= 2 && legalKingMoveCount(board, color) <= 1) {
            score += PALACE_UNBLOCK_BONUS;
        }
        return Math.min(22_000, score);
    }

    private int legalKingMoveCount(Board board, PlayerColor color) {
        int count = 0;
        for (Move action : moveGenerator.generateActions(board, color, System.currentTimeMillis())) {
            Piece mover = board.get(action.source());
            if (mover != null && mover.visible() && mover.type() == PieceType.KING
                    && ruleEngine.canMoveAndKeepKingSafe(board, action.source(), action.destination(), color)) {
                count++;
            }
        }
        return count;
    }

    private int ownPalaceOccupancy(Board board, PlayerColor color) {
        int count = 0;
        for (int y = color == PlayerColor.RED ? 0 : 7; y <= (color == PlayerColor.RED ? 2 : 9); y++) {
            for (int x = 3; x <= 5; x++) {
                Piece piece = board.get(new Position(x, y));
                if (piece != null && piece.color() == color) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean ownPalace(Position position, PlayerColor color) {
        return position.x() >= 3 && position.x() <= 5
                && (color == PlayerColor.RED ? position.y() >= 0 && position.y() <= 2
                : position.y() >= 7 && position.y() <= 9);
    }

    private boolean sameFileOrRank(Position first, Position second) {
        return first.x() == second.x() || first.y() == second.y();
    }

    private boolean isBetween(Position first, Position middle, Position second) {
        if (!sameFileOrRank(first, second) || !sameFileOrRank(first, middle)
                || !sameFileOrRank(middle, second)) {
            return false;
        }
        if (first.x() == second.x()) {
            return middle.x() == first.x()
                    && middle.y() > Math.min(first.y(), second.y())
                    && middle.y() < Math.max(first.y(), second.y());
        }
        return middle.y() == first.y()
                && middle.x() > Math.min(first.x(), second.x())
                && middle.x() < Math.max(first.x(), second.x());
    }

    private Piece singlePieceBetween(Board board, Position first, Position second) {
        if (!sameFileOrRank(first, second)) {
            return null;
        }
        int dx = Integer.compare(second.x(), first.x());
        int dy = Integer.compare(second.y(), first.y());
        Piece found = null;
        int x = first.x() + dx;
        int y = first.y() + dy;
        while (x != second.x() || y != second.y()) {
            Piece piece = board.get(new Position(x, y));
            if (piece != null) {
                if (found != null) {
                    return null;
                }
                found = piece;
            }
            x += dx;
            y += dy;
        }
        return found;
    }

    private Position singlePieceBetweenPosition(Board board, Position first, Position second) {
        if (!sameFileOrRank(first, second)) {
            return null;
        }
        int dx = Integer.compare(second.x(), first.x());
        int dy = Integer.compare(second.y(), first.y());
        Position found = null;
        int x = first.x() + dx;
        int y = first.y() + dy;
        while (x != second.x() || y != second.y()) {
            Position current = new Position(x, y);
            Piece piece = board.get(current);
            if (piece != null) {
                if (found != null) {
                    return null;
                }
                found = current;
            }
            x += dx;
            y += dy;
        }
        return found;
    }

    private boolean boardControlsPalace(Board board, Position source, PlayerColor palaceOwner) {
        Piece piece = board.get(source);
        if (piece == null || piece.color() == palaceOwner) {
            return false;
        }
        for (int y = palaceOwner == PlayerColor.RED ? 0 : 7; y <= (palaceOwner == PlayerColor.RED ? 2 : 9); y++) {
            for (int x = 3; x <= 5; x++) {
                Position target = new Position(x, y);
                if (source.equals(target)) {
                    continue;
                }
                if (ruleEngine.canMove(board, source, target, piece.color())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int manhattan(Position first, Position second) {
        return Math.abs(first.x() - second.x()) + Math.abs(first.y() - second.y());
    }

    private int kingAttackProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
            return 0;
        }
        int progress = majorKingAttackScore(after, color) - majorKingAttackScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        int score = Math.min(KING_ATTACK_PROGRESS_BONUS, Math.max(0, progress - 220) * 3);
        if (isOpponentSide(move.destination(), color)) {
            score += 220;
        }
        if (ruleEngine.isInCheck(after, color.opponent())) {
            score += moveGenerator.hasCheckEscape(after, color.opponent()) ? 180 : WIN_SCORE / 5;
        }
        return score;
    }

    private int endgameMateNetProgressScore(Board before, Board after, Move move, PlayerColor color) {
        int progress = endgameMateNetScore(after, color) - endgameMateNetScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(ENDGAME_MATE_NET_PROGRESS_BONUS, progress);
    }

    private int endgameMateNetScore(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("mateNet", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (strategicPhase(board, color) < LATE_MAJOR_PHASE - 8) {
            return 0;
        }
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        int attackers = 0;
        int escapeCount = legalKingMoveCount(board, color.opponent());
        if (escapeCount <= 1) {
            score += 1_600;
        } else if (escapeCount == 2) {
            score += 720;
        }
        if (ruleEngine.isInCheck(board, color.opponent())) {
            score += moveGenerator.hasCheckEscape(board, color.opponent()) ? 650 : WIN_SCORE / 5;
        }
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            int contribution = piecePressureContribution(board, source, type, enemyKing, color);
            if (contribution <= 0) {
                continue;
            }
            if (type == PieceType.ROOK || type == PieceType.CANNON
                    || type == PieceType.KNIGHT || type == PieceType.PAWN) {
                attackers++;
                score += contribution;
            }
        }
        if (attackers >= 3) {
            score += 1_000;
        } else if (attackers == 2) {
            score += 520;
        }
        score += cannonScreenScore(board, color) / 2;
        score += knightKillShapeScore(board, color) / 2;
        score += pawnConstrictionScore(board, color) / 2;
        int result = Math.min(8_000, score);
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int endgameTechniqueScore(Board board, PlayerColor color) {
        if (strategicPhase(board, color) < LATE_MAJOR_PHASE) {
            return 0;
        }
        String cacheKey = heuristicKey("endTech", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int material = materialBalance(board, color);
        int score = 0;
        int majors = 0;
        int advancedPawns = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT) {
                majors++;
                if (sameFileOrRank(source, enemyKing) || boardControlsPalace(board, source, color.opponent())) {
                    score += 520;
                }
                if (attackersValue(board, source, color.opponent()) > 0
                        && defendersValue(board, source, color) == 0) {
                    score -= material > 0 ? 900 : 1_200;
                }
            } else if (type == PieceType.PAWN && isOpponentSide(source, color)) {
                advancedPawns++;
                score += distanceToPalace(source, color.opponent()) <= 2 ? 520 : 220;
            }
        }
        if (material > 900) {
            score += Math.max(0, 3 - majors) * -420;
            score += endgameMateNetScore(board, color) / 2;
        } else if (material < -700) {
            score += majors * 260 + advancedPawns * 180;
            score -= Math.max(0, opponentPlanThreatScore(board, color) - 4_000) / 3;
        }
        if (legalKingMoveCount(board, color.opponent()) <= 1) {
            score += 900;
        }
        int result = Math.max(-3_500, Math.min(4_800, score));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int activeBreakthroughScore(Board before, Board after, Move move, PlayerColor color) {
        if (ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        int attackGain = coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color);
        int netGain = endgameMateNetScore(after, color) - endgameMateNetScore(before, color);
        int pressureGain = majorKingAttackScore(after, color) - majorKingAttackScore(before, color);
        int exchangeGain = 0;
        Piece captured = before.get(move.destination());
        if (captured != null) {
            exchangeGain = captureValue(before, captured, move.destination())
                    - pieceSearchValue(before, mover, move.source()) / 3;
        }
        int score = Math.max(0, attackGain)
                + Math.max(0, netGain)
                + Math.max(0, pressureGain)
                + Math.max(0, exchangeGain);
        if (isOpponentSide(move.destination(), color)) {
            score += 220;
        }
        if (captured != null && exchangeSequenceGain(after, move.destination(), color.opponent()) <= exchangeGain + 160) {
            score += 420;
        }
        return Math.min(ACTIVE_BREAKTHROUGH_BONUS, Math.max(0, score / 2));
    }

    private int activePieceActivationScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(mover) == PieceType.KING) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        Position enemyKing = before.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        PieceType type = knownType(mover);
        int score = 0;
        int beforeRole = pieceRoleScore(before, color);
        int afterRole = pieceRoleScore(after, color);
        score += Math.max(0, afterRole - beforeRole);
        score += Math.max(0, layoutPatternScore(after, color) - layoutPatternScore(before, color)) / 2;
        score += Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color)) / 2;
        score += Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color)) / 3;
        if (isOpponentSide(move.destination(), color) && !isOpponentSide(move.source(), color)) {
            score += switch (type) {
                case ROOK, CANNON, KNIGHT -> 650;
                case PAWN -> 520;
                case GUARD, BISHOP -> 180;
                case KING -> 0;
            };
        }
        int rankGain = forwardRank(move.destination(), color) - forwardRank(move.source(), color);
        if (rankGain > 0) {
            score += switch (type) {
                case ROOK -> Math.min(900, rankGain * 260);
                case CANNON -> Math.min(620, rankGain * 180);
                case KNIGHT -> Math.min(560, rankGain * 160);
                case PAWN -> Math.min(520, rankGain * 150);
                case GUARD, BISHOP -> Math.min(240, rankGain * 80);
                case KING -> 0;
            };
        }
        if ((type == PieceType.ROOK || type == PieceType.CANNON) && sameFileOrRank(move.destination(), enemyKing)) {
            int between = after.countBetween(move.destination(), enemyKing);
            score += type == PieceType.ROOK
                    ? (between <= 1 ? 1_000 : 360)
                    : (between == 1 ? 1_000 : between <= 2 ? 420 : 120);
        }
        if ((type == PieceType.ROOK || type == PieceType.CANNON)
                && openLineMobility(after, move.destination()) > openLineMobility(before, move.source())) {
            score += 420;
        }
        if (type == PieceType.CANNON) {
            score += Math.max(0, cannonScreenScore(after, color) - cannonScreenScore(before, color)) / 2;
            score += cannonUnlockProgressScore(before, after, move, color) / 2;
        } else if (type == PieceType.KNIGHT) {
            score += knightKillShapeProgressScore(before, after, move, color);
            int beforeDistance = manhattan(move.source(), enemyKing);
            int afterDistance = manhattan(move.destination(), enemyKing);
            if (afterDistance < beforeDistance) {
                score += Math.min(700, (beforeDistance - afterDistance) * 180);
            }
        } else if (type == PieceType.PAWN) {
            score += pawnConstrictionProgressScore(before, after, move, color);
            score += pawnTrapProgressScore(before, after, move, color);
            if (isOpponentSide(move.destination(), color)) {
                score += 360;
            }
            score += nearKingMinorPressureProgress(before, after, move, color);
        } else if (type == PieceType.GUARD || type == PieceType.BISHOP) {
            score += Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color)) / 2;
            if (ownPalace(move.source(), color) && !ownPalace(move.destination(), color)
                    && palaceBlockadeRisk(before, color) > palaceBlockadeRisk(after, color)) {
                score += PALACE_UNBLOCK_BONUS / 2;
            }
            score += nearKingMinorPressureProgress(before, after, move, color);
        }
        if (ownPalace(move.source(), color) && !ownPalace(move.destination(), color)
                && palaceBlockadeRisk(before, color) > 0) {
            score += PALACE_UNBLOCK_BONUS / 2;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            score += Math.max(0, captureValue(before, captured, move.destination())
                    - pieceSearchValue(before, mover, move.source()) / 3);
        }
        if (legalAttackersValue(after, move.destination(), color.opponent()) > 0
                && defendersValue(after, move.destination(), color) == 0) {
            score -= pieceSearchValue(after, moved, move.destination());
        }
        int cap = switch (type) {
            case ROOK -> ACTIVE_PIECE_ACTIVATION_BONUS;
            case CANNON -> ACTIVE_PIECE_ACTIVATION_BONUS - 200;
            case KNIGHT -> ACTIVE_PIECE_ACTIVATION_BONUS - 300;
            case PAWN -> ACTIVE_PIECE_ACTIVATION_BONUS - 650;
            case GUARD, BISHOP -> ACTIVE_PIECE_ACTIVATION_BONUS - 850;
            case KING -> 0;
        };
        return Math.max(0, Math.min(cap, score));
    }

    private int nearKingMinorPressureProgress(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.PAWN && type != PieceType.GUARD && type != PieceType.BISHOP) {
            return 0;
        }
        Position enemyKing = before.findKing(color.opponent());
        if (enemyKing == null || !isOpponentSide(move.destination(), color)) {
            return 0;
        }
        int beforeDistance = manhattan(move.source(), enemyKing);
        int afterDistance = manhattan(move.destination(), enemyKing);
        int score = 0;
        if (afterDistance < beforeDistance) {
            score += (beforeDistance - afterDistance) * (type == PieceType.PAWN ? 240 : 160);
        }
        if (distanceToPalace(move.destination(), color.opponent()) <= 1) {
            score += type == PieceType.PAWN ? 520 : 360;
        }
        if (ruleEngine.isInCheck(after, color.opponent())) {
            score += moveGenerator.hasCheckEscape(after, color.opponent()) ? 420 : WIN_SCORE / 5;
        }
        int escapeLoss = legalKingMoveCount(before, color.opponent()) - legalKingMoveCount(after, color.opponent());
        if (escapeLoss > 0) {
            score += escapeLoss * 680;
        }
        score += Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color)) / 2;
        return Math.min(1_600, Math.max(0, score));
    }

    private int nearKingMinorDriftPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || before.get(move.destination()) != null) {
            return 0;
        }
        PieceType type = knownType(mover);
        if (type != PieceType.PAWN && type != PieceType.GUARD && type != PieceType.BISHOP) {
            return 0;
        }
        Position enemyKing = before.findKing(color.opponent());
        if (enemyKing == null || !isOpponentSide(move.source(), color)) {
            return 0;
        }
        int beforeDistance = manhattan(move.source(), enemyKing);
        if (beforeDistance > 4 && distanceToPalace(move.source(), color.opponent()) > 2) {
            return 0;
        }
        if (ruleEngine.isInCheck(before, color)
                || (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent()))) {
            return 0;
        }
        int pressureProgress = nearKingMinorPressureProgress(before, after, move, color);
        int defenseRelief = Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color))
                + Math.max(0, opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color));
        if (pressureProgress >= 360 || defenseRelief >= 900) {
            return 0;
        }
        int afterDistance = manhattan(move.destination(), enemyKing);
        int penalty = NEAR_KING_MINOR_DRIFT_PENALTY;
        if (afterDistance >= beforeDistance) {
            penalty += 700;
        }
        if (type == PieceType.PAWN) {
            penalty += 300;
        }
        return Math.min(6_000, penalty);
    }

    private int realizablePlanProgressScore(Board before, Board after, Move move, PlayerColor color) {
        if (ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.KING) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return REALIZABLE_PLAN_BONUS;
        }
        int followup = bestContinuationLineValue(after, color);
        int opponentFollowup = bestOnePlyContinuationValue(after, color.opponent(), 3);
        int progress = followup
                + Math.max(0, candidatePlanScore(after, color) - candidatePlanScore(before, color)) / 2
                + Math.max(0, strategicPlanPotential(after, color) - strategicPlanPotential(before, color)) / 2
                - opponentFollowup / 3;
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            progress += captureValue(before, captured, move.destination()) / 2;
        }
        if (progress < 900) {
            return 0;
        }
        return Math.min(REALIZABLE_PLAN_BONUS, progress / 2);
    }

    private int noFollowupDriftPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (ruleEngine.isInCheck(before, color)
                || (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent()))) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.KING) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            return 0;
        }
        int defenseRelief = kingDangerScore(before, color) - kingDangerScore(after, color)
                + opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color);
        if (defenseRelief >= 1_000 || palaceUnblockProgressScore(before, after, move, color) >= PALACE_UNBLOCK_BONUS / 2) {
            return 0;
        }
        int followup = bestContinuationLineValue(after, color);
        int planGain = candidatePlanScore(after, color) - candidatePlanScore(before, color)
                + strategicPlanPotential(after, color) - strategicPlanPotential(before, color);
        int activation = activePieceActivationScore(before, after, move, color);
        if (followup + planGain + activation >= 1_400) {
            return 0;
        }
        int penalty = NO_FOLLOWUP_DRIFT_PENALTY;
        if (ruleEngine.isInCheck(after, color.opponent()) && moveGenerator.hasCheckEscape(after, color.opponent())) {
            penalty += NON_DECISIVE_CHECK_PENALTY;
        }
        if (knownType(mover) == PieceType.ROOK || knownType(mover) == PieceType.CANNON || knownType(mover) == PieceType.KNIGHT) {
            penalty += 900;
        }
        if (visiblePhase(before) >= LATE_MAJOR_PHASE) {
            penalty += 700;
        }
        return Math.min(9_000, penalty);
    }

    private int bestContinuationLineValue(Board board, PlayerColor color) {
        String cacheKey = "continuationLine|" + color.name() + "|" + knownPositionKey(board);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int best = 0;
        int searched = 0;
        for (Move followup : lightweightIntentMoves(board, color)) {
            if (searched++ >= 4) {
                break;
            }
            Board next = applyForSearch(board, followup);
            int score = immediateContinuationMoveValue(board, next, followup, color);
            if (Math.abs(score) < WIN_SCORE / 5) {
                int opponentReply = bestOnePlyContinuationValue(next, color.opponent(), 3);
                int secondFollow = opponentReply < WIN_SCORE / 5
                        ? bestSecondFollowAfterLikelyReply(next, color)
                        : 0;
                score = score - opponentReply / 2 + secondFollow / 3;
            }
            best = Math.max(best, score);
        }
        int result = Math.min(6_000, Math.max(0, best));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int bestSecondFollowAfterLikelyReply(Board board, PlayerColor color) {
        int best = 0;
        int searched = 0;
        for (Move reply : lightweightIntentMoves(board, color.opponent())) {
            if (searched++ >= 2) {
                break;
            }
            Board afterReply = applyForSearch(board, reply);
            best = Math.max(best, bestOnePlyContinuationValue(afterReply, color, 3));
        }
        return best;
    }

    private int bestOnePlyContinuationValue(Board board, PlayerColor color, int limit) {
        int best = 0;
        int searched = 0;
        for (Move move : lightweightIntentMoves(board, color)) {
            if (searched++ >= limit) {
                break;
            }
            Board next = applyForSearch(board, move);
            best = Math.max(best, immediateContinuationMoveValue(board, next, move, color));
        }
        return best;
    }

    private int immediateContinuationMoveValue(Board before, Board after, Move move, PlayerColor color) {
        int score = 0;
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            score += captureValue(before, captured, move.destination()) * 2;
        }
        if (ruleEngine.isInCheck(after, color.opponent())) {
            score += moveGenerator.hasCheckEscape(after, color.opponent()) ? 700 : WIN_SCORE / 4;
        }
        score += Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color));
        score += Math.max(0, layoutPatternScore(after, color) - layoutPatternScore(before, color));
        score += Math.max(0, pieceRoleScore(after, color) - pieceRoleScore(before, color)) / 2;
        score += Math.max(0, palaceBlockadeRisk(before, color) - palaceBlockadeRisk(after, color));
        score += activePieceActivationScore(before, after, move, color) / 2;
        score -= exposedMovePenalty(before, after, move, color) / 2;
        score -= urgentTradePenalty(before, after, move, color, false) / 3;
        return score;
    }

    private int flyingKingCoordinationProgressScore(Board before, Board after, Move move, PlayerColor color) {
        int progress = flyingKingCoordinationScore(after, color) - flyingKingCoordinationScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(FLYING_KING_COORDINATION_BONUS, progress);
    }

    private int flyingKingCoordinationScore(Board board, PlayerColor color) {
        Position ownKing = board.findKing(color);
        Position enemyKing = board.findKing(color.opponent());
        if (ownKing == null || enemyKing == null || ownKing.x() != enemyKing.x()) {
            return 0;
        }
        int betweenKings = board.countBetween(ownKing, enemyKing);
        int score = betweenKings == 0 ? WIN_SCORE / 5 : betweenKings == 1 ? 900 : betweenKings == 2 ? 420 : 0;
        if (score == 0) {
            return 0;
        }
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type == PieceType.ROOK || type == PieceType.CANNON) {
                if (source.x() == enemyKing.x() || source.y() == enemyKing.y()) {
                    int between = board.countBetween(source, enemyKing);
                    score += type == PieceType.ROOK
                            ? (between <= 1 ? 520 : 160)
                            : (between == 1 ? 680 : between == 2 ? 220 : 0);
                }
            } else if (type == PieceType.KNIGHT && manhattan(source, enemyKing) <= 4) {
                score += 260;
            } else if (type == PieceType.PAWN && isOpponentSide(source, color)
                    && Math.abs(source.x() - enemyKing.x()) <= 1) {
                score += 320;
            }
        }
        return Math.min(3_600, score);
    }

    private int pawnAttackProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) != PieceType.PAWN) {
            return 0;
        }
        Position enemyKing = before.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int beforeDistance = manhattan(move.source(), enemyKing);
        int afterDistance = manhattan(move.destination(), enemyKing);
        int score = Math.max(0, beforeDistance - afterDistance) * 120;
        score += Math.max(0, 7 - afterDistance) * 140;
        if (isOpponentSide(move.destination(), color)) {
            score += 180;
        }
        if (Math.abs(move.destination().x() - enemyKing.x()) <= 1
                && Math.abs(move.destination().y() - enemyKing.y()) <= 3) {
            score += 280;
        }
        if (boardControlsPalace(after, move.destination(), color.opponent())) {
            score += 240;
        }
        return score;
    }

    private int pawnTrapProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(mover) != PieceType.PAWN) {
            return 0;
        }
        int beforeScore = pawnTrapScore(before, move.source(), color);
        int afterScore = pawnTrapScore(after, move.destination(), color);
        int progress = afterScore - beforeScore;
        if (progress <= 0) {
            return 0;
        }
        return Math.min(PAWN_TRAP_BONUS, progress);
    }

    private int pawnTrapScore(Board board, Position pawnPosition, PlayerColor color) {
        Piece pawn = board.get(pawnPosition);
        if (pawn == null || pawn.color() != color || knownType(pawn) != PieceType.PAWN
                || !isOpponentSide(pawnPosition, color)) {
            return 0;
        }
        Position forcedTarget = pawnForwardTarget(pawnPosition, color);
        int score = 0;
        if (forcedTarget != null) {
            Piece target = board.get(forcedTarget);
            if (target != null && target.color() == color.opponent()) {
                int targetValue = captureValue(board, target, forcedTarget);
                score += Math.max(260, targetValue / 2);
                if (!target.visible()) {
                    score += remainingHighValueHiddenCount(board, color.opponent()) * 130;
                }
            }
        }
        int opponentTakesPawn = legalAttackersValue(board, pawnPosition, color.opponent());
        if (opponentTakesPawn == 0) {
            return score;
        }
        int recaptureValue = recaptureTrapValue(board, pawnPosition, color);
        if (recaptureValue == 0) {
            return score;
        }
        score += PAWN_TRAP_BONUS + recaptureValue / 2;
        if (opponentTakesPawn >= HIGH_VALUE_PIECE) {
            score += 520;
        }
        return Math.min(2_800, score);
    }

    private Position pawnForwardTarget(Position pawnPosition, PlayerColor color) {
        int y = pawnPosition.y() + color.forwardDirection();
        if (!Position.isInside(pawnPosition.x(), y)) {
            return null;
        }
        return new Position(pawnPosition.x(), y);
    }

    private int recaptureTrapValue(Board board, Position baitPosition, PlayerColor color) {
        int best = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.PAWN
                    || knownType(piece) == PieceType.KING) {
                continue;
            }
            if (ruleEngine.canMove(board, source, baitPosition, color)) {
                best = Math.max(best, pieceSearchValue(board, piece, source));
            }
        }
        return best;
    }

    private int advancedPawnKingThreatScore(Board board, PlayerColor color) {
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) != PieceType.PAWN
                    || !isOpponentSide(source, color)) {
                continue;
            }
            int distance = manhattan(source, enemyKing);
            score += Math.max(0, 8 - distance) * 150;
            if (source.x() == enemyKing.x()) {
                score += 620;
            }
            if (Math.abs(source.x() - enemyKing.x()) <= 1 && Math.abs(source.y() - enemyKing.y()) <= 3) {
                score += 760;
            }
            if (ownPalace(source, color.opponent())) {
                score += 1_100;
            }
            Position next = pawnForwardTarget(source, color);
            if (next != null && ownPalace(next, color.opponent())) {
                score += 650;
            }
            if (legalAttackersValue(board, source, color.opponent()) == 0) {
                score += 260;
            }
        }
        return Math.min(4_000, score);
    }

    private int majorKingAttackScore(Board board, PlayerColor color) {
        Position king = board.findKing(color.opponent());
        if (king == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
                continue;
            }
            int distance = Math.abs(source.x() - king.x()) + Math.abs(source.y() - king.y());
            if (source.x() == king.x() || source.y() == king.y()) {
                score += switch (type) {
                    case ROOK -> board.countBetween(source, king) <= 1 ? 520 : 220;
                    case CANNON -> board.countBetween(source, king) == 1 ? 620 : 180;
                    case KNIGHT -> 120;
                    default -> 0;
                };
            }
            if (type == PieceType.KNIGHT && distance <= 4 && isOpponentSide(source, color)) {
                score += Math.max(0, 5 - distance) * 90;
            }
            if ((type == PieceType.ROOK || type == PieceType.CANNON)
                    && Math.abs(source.x() - king.x()) <= 1
                    && isOpponentSide(source, color)) {
                score += 120;
            }
        }
        return Math.min(4_000, score);
    }

    private boolean canOpponentKingSafelyCapture(Board board, Position target, PlayerColor color) {
        PlayerColor opponent = color.opponent();
        Position king = board.findKing(opponent);
        return king != null && ruleEngine.canMoveAndKeepKingSafe(board, king, target, opponent);
    }

    private int cannonUnlockProgressScore(Board before, Board after, Move move, PlayerColor color) {
        int beforeLocked = flyingKingLockedCannonPressure(before, color);
        if (beforeLocked <= 0) {
            return 0;
        }
        int afterLocked = flyingKingLockedCannonPressure(after, color);
        int unlocked = beforeLocked - afterLocked;
        if (unlocked <= 0) {
            return 0;
        }
        Piece mover = before.get(move.source());
        int score = Math.min(CANNON_UNLOCK_BONUS * 2, unlocked * 420);
        if (mover != null && knownType(mover) == PieceType.KING) {
            score += CANNON_UNLOCK_BONUS;
        } else {
            score += CANNON_UNLOCK_BONUS / 2;
        }
        score += Math.max(0, kingPressure(after, color) - kingPressure(before, color)) * 2;
        score += Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color)) / 2;
        return Math.min(3_800, score);
    }

    private int cannonScreenProgressScore(Board before, Board after, Move move, PlayerColor color) {
        int progress = cannonScreenScore(after, color) - cannonScreenScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover != null && knownType(mover) != PieceType.CANNON
                && createsCannonScreen(after, move.destination(), color)) {
            progress += 420;
        }
        return Math.min(CANNON_SCREEN_PROGRESS_BONUS, progress);
    }

    private int cannonScreenScore(Board board, PlayerColor color) {
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        for (Position cannon : board.occupiedPositions()) {
            Piece piece = board.get(cannon);
            if (piece == null || piece.color() != color || knownType(piece) != PieceType.CANNON) {
                continue;
            }
            score += openLineMobility(board, cannon) * 24;
            if (sameFileOrRank(cannon, enemyKing)) {
                int between = board.countBetween(cannon, enemyKing);
                if (between == 1) {
                    score += 1_180;
                    Piece screen = singlePieceBetween(board, cannon, enemyKing);
                    if (screen != null && screen.color() == color) {
                        score += 220;
                    }
                } else if (between == 2) {
                    score += 380;
                } else if (between == 0) {
                    score += 120;
                }
            } else if (Math.abs(cannon.x() - enemyKing.x()) <= 1
                    || Math.abs(cannon.y() - enemyKing.y()) <= 1) {
                score += 180;
            }
        }
        return Math.min(3_800, score);
    }

    private boolean createsCannonScreen(Board board, Position screen, PlayerColor color) {
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return false;
        }
        Piece screenPiece = board.get(screen);
        if (screenPiece == null || screenPiece.color() != color) {
            return false;
        }
        for (Position cannon : board.occupiedPositions()) {
            Piece piece = board.get(cannon);
            if (piece == null || piece.color() != color || knownType(piece) != PieceType.CANNON) {
                continue;
            }
            if (sameFileOrRank(cannon, enemyKing)
                    && isBetween(cannon, screen, enemyKing)
                    && board.countBetween(cannon, enemyKing) == 1) {
                return true;
            }
        }
        return false;
    }

    private int knightKillShapeProgressScore(Board before, Board after, Move move, PlayerColor color) {
        int progress = knightKillShapeScore(after, color) - knightKillShapeScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(KNIGHT_KILL_SHAPE_PROGRESS_BONUS, progress);
    }

    private int knightKillShapeScore(Board board, PlayerColor color) {
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        int majorSupport = majorKingAttackScore(board, color);
        for (Position knight : board.occupiedPositions()) {
            Piece piece = board.get(knight);
            if (piece == null || piece.color() != color || knownType(piece) != PieceType.KNIGHT) {
                continue;
            }
            int distance = manhattan(knight, enemyKing);
            if (!isOpponentSide(knight, color) && distance > 4) {
                continue;
            }
            int controlledEscapes = controlledPalaceSquaresByKnight(board, knight, color, color.opponent());
            if (ruleEngine.canMove(board, knight, enemyKing, color)) {
                score += 1_050;
            }
            score += controlledEscapes * 260;
            score += Math.max(0, 6 - distance) * 120;
            if (majorSupport >= 900 && controlledEscapes >= 2) {
                score += 620;
            } else if (majorSupport >= 500 && controlledEscapes >= 1) {
                score += 320;
            }
            if (blockedKnightLegs(board, knight) == 0 && distance <= 5) {
                score += 260;
            }
        }
        return Math.min(3_600, score);
    }

    private int controlledPalaceSquaresByKnight(
            Board board,
            Position knight,
            PlayerColor color,
            PlayerColor palaceOwner) {
        int count = 0;
        int minY = palaceOwner == PlayerColor.RED ? 0 : 7;
        int maxY = palaceOwner == PlayerColor.RED ? 2 : 9;
        for (int y = minY; y <= maxY; y++) {
            for (int x = 3; x <= 5; x++) {
                Position target = new Position(x, y);
                Piece occupant = board.get(target);
                if (occupant != null && occupant.color() == color) {
                    continue;
                }
                if (ruleEngine.canMove(board, knight, target, color)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int flyingKingLockedCannonPressure(Board board, PlayerColor color) {
        Position ownKing = board.findKing(color);
        Position enemyKing = board.findKing(color.opponent());
        if (ownKing == null || enemyKing == null || ownKing.x() != enemyKing.x()) {
            return 0;
        }
        int betweenKings = board.countBetween(ownKing, enemyKing);
        if (betweenKings > 1) {
            return 0;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) != PieceType.CANNON) {
                continue;
            }
            int kingDistance = manhattan(source, enemyKing);
            if (source.x() == enemyKing.x()) {
                score += 3 + Math.max(0, 6 - kingDistance);
            } else if (Math.abs(source.x() - enemyKing.x()) <= 2 && Math.abs(source.y() - enemyKing.y()) <= 3) {
                score += 2;
            }
        }
        return score;
    }

    private int urgentDefenseScore(Board before, Board after, Move move, PlayerColor color) {
        int beforeExposure = importantPieceExposure(before, color);
        int afterExposure = importantPieceExposure(after, color);
        int score = beforeExposure - afterExposure;
        int planRelief = opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color);
        if (planRelief > 0) {
            score += planRelief * 2 + defensivePlanBreakScore(before, after, move, color);
        }
        boolean unsafeLowValueCapture = unsafeMajorForLowValueCapture(before, after, move, color);
        if (!unsafeLowValueCapture) {
            score += invadingPieceScore(before, color) - invadingPieceScore(after, color);
        }

        Piece mover = before.get(move.source());
        if (mover != null && pieceSearchValue(before, mover, move.source()) >= HIGH_VALUE_PIECE
                && attackersValue(before, move.source(), color.opponent()) > 0
                && attackersValue(after, move.destination(), color.opponent()) == 0) {
            score += THREATENED_MAJOR_ESCAPE_BONUS;
        }

        Piece captured = before.get(move.destination());
        if (!unsafeLowValueCapture && captured != null && captured.color() == color.opponent()
                && threatensImportantPiece(before, move.destination(), color.opponent(), color)) {
            score += CAPTURE_THREATENING_PIECE_BONUS;
        }
        if (!unsafeLowValueCapture && captured != null && captured.color() == color.opponent()
                && captureValue(before, captured, move.destination()) >= HIGH_VALUE_PIECE
                && isHomeSide(move.destination(), color)
                && mover != null && !mover.visible()) {
            score += HOME_DEFENSE_CAPTURE_BONUS;
        }
        if (!unsafeLowValueCapture) {
            score += defensiveReliefScore(before, after, move, color);
        }
        score += specificDefenseReliefScore(before, after, move, color);
        return score;
    }

    private int specificDefenseReliefScore(Board before, Board after, Move move, PlayerColor color) {
        int beforeThreat = specificThreatShapeScore(before, color);
        int afterThreat = specificThreatShapeScore(after, color);
        int relief = beforeThreat - afterThreat;
        if (relief <= 0) {
            return 0;
        }
        Piece mover = before.get(move.source());
        int score = relief * 2;
        if (mover != null) {
            PieceType type = knownType(mover);
            if (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT) {
                score += 700;
            } else if (type == PieceType.PAWN || type == PieceType.GUARD || type == PieceType.BISHOP) {
                score += 320;
            }
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            score += replayLikeThreatValue(knownType(captured));
        }
        return Math.min(5_500, score);
    }

    private int specificThreatShapeScore(Board board, PlayerColor defender) {
        Position king = board.findKing(defender);
        if (king == null) {
            return WIN_SCORE / 2;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != defender.opponent()) {
                continue;
            }
            PieceType type = knownType(piece);
            int distance = manhattan(source, king);
            if (type == PieceType.ROOK && sameFileOrRank(source, king)) {
                int between = board.countBetween(source, king);
                score += between == 0 ? 2_600 : between == 1 ? 1_300 : between == 2 ? 360 : 0;
            } else if (type == PieceType.CANNON && sameFileOrRank(source, king)) {
                int between = board.countBetween(source, king);
                score += between == 1 ? 2_700 : between == 2 ? 1_050 : between == 0 ? 260 : 0;
            } else if (type == PieceType.KNIGHT && distance <= 4) {
                score += Math.max(0, 5 - distance) * 360;
            } else if (type == PieceType.PAWN && (ownPalace(source, defender) || distance <= 2)) {
                score += 1_050 + Math.max(0, 4 - distance) * 260;
            }
        }
        return Math.min(8_000, score);
    }

    private int replayLikeThreatValue(PieceType type) {
        return switch (type) {
            case ROOK -> 950;
            case CANNON -> 720;
            case KNIGHT -> 620;
            case PAWN -> 360;
            case GUARD, BISHOP -> 180;
            case KING -> WIN_SCORE / 5;
        };
    }

    private int defensiveReliefScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        if (unsafeMajorForLowValueCapture(before, after, move, color)) {
            return 0;
        }
        int invaderRelief = invadingPieceScore(before, color) - invadingPieceScore(after, color);
        int kingRelief = kingPressure(before, color.opponent()) - kingPressure(after, color.opponent());
        int planRelief = opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color);
        int score = Math.max(0, invaderRelief) + Math.max(0, kingRelief) * 2;
        if (planRelief > 0) {
            score += planRelief * 2;
        }

        PieceType moverType = knownType(mover);
        if (moverType == PieceType.ROOK || moverType == PieceType.CANNON || moverType == PieceType.KNIGHT) {
            if (score > 0) {
                score += DEFENSIVE_MAJOR_RELIEF_BONUS;
            }
            Position king = before.findKing(color);
            if (king != null && isHomeSide(move.destination(), color)
                    && manhattan(move.destination(), king) <= 4) {
                score += 420;
            }
        }

        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            score += homeInvaderDanger(before, move.destination(), color) * 2;
        }
        return Math.min(8_000, Math.max(0, score));
    }

    private int defensivePlanBreakScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        Position king = before.findKing(color);
        if (king == null) {
            return 0;
        }
        int score = 0;
        PieceType moverType = knownType(mover);
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            int danger = homeInvaderDanger(before, move.destination(), color);
            if (danger > 0 || boardControlsPalace(before, move.destination(), color)) {
                score += 1_250 + danger;
            }
            PieceType capturedType = knownType(captured);
            if (capturedType == PieceType.ROOK || capturedType == PieceType.CANNON || capturedType == PieceType.KNIGHT) {
                score += 1_100;
            } else if (capturedType == PieceType.PAWN && distanceToPalace(move.destination(), color) <= 2) {
                score += 780;
            }
        }

        if (moverType == PieceType.ROOK || moverType == PieceType.CANNON || moverType == PieceType.KNIGHT) {
            score += DEFENSIVE_MAJOR_RELIEF_BONUS / 2;
        }
        if (moverType != PieceType.KING && isHomeSide(move.destination(), color)
                && manhattan(move.destination(), king) <= 4) {
            score += 520;
        }
        score += lineThreatBreakScore(before, after, color);
        return Math.min(6_000, Math.max(0, score));
    }

    private int lineThreatBreakScore(Board before, Board after, PlayerColor defender) {
        Position beforeKing = before.findKing(defender);
        Position afterKing = after.findKing(defender);
        if (beforeKing == null || afterKing == null) {
            return 0;
        }
        int beforeLines = liveLineThreatCount(before, beforeKing, defender);
        int afterLines = liveLineThreatCount(after, afterKing, defender);
        if (afterLines >= beforeLines) {
            return 0;
        }
        return (beforeLines - afterLines) * 1_150;
    }

    private int liveLineThreatCount(Board board, Position king, PlayerColor defender) {
        int count = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != defender.opponent()) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type != PieceType.ROOK && type != PieceType.CANNON) {
                continue;
            }
            if (!sameFileOrRank(source, king)) {
                continue;
            }
            int between = board.countBetween(source, king);
            if ((type == PieceType.ROOK && between <= 1)
                    || (type == PieceType.CANNON && between <= 2)) {
                count++;
            }
        }
        return count;
    }

    private int opponentPlanThreatScore(Board board, PlayerColor defender) {
        String cacheKey = heuristicKey("oppPlan", board, defender);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Position king = board.findKing(defender);
        if (king == null) {
            return WIN_SCORE / 2;
        }
        PlayerColor attacker = defender.opponent();
        int score = Math.max(0, layoutPatternScore(board, attacker)) / 2
                + Math.max(0, coordinatedKingAttackScore(board, attacker)) / 2
                + rookCannonBatteryThreatScore(board, defender);
        int nearbyAttackers = 0;
        int palacePressure = 0;
        boolean rookLike = false;
        boolean cannonLike = false;
        boolean pawnNet = false;
        boolean kingBacked = false;
        Position attackerKing = board.findKing(attacker);

        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != attacker) {
                continue;
            }
            PieceType type = knownType(piece);
            int distance = manhattan(source, king);
            if (distance <= 5 || boardControlsPalace(board, source, defender)) {
                nearbyAttackers++;
            }
            if (boardControlsPalace(board, source, defender)) {
                palacePressure += switch (type) {
                    case ROOK -> 720;
                    case CANNON -> 820;
                    case KNIGHT -> 560;
                    case PAWN -> 640;
                    case GUARD, BISHOP -> 220;
                    case KING -> 0;
                };
            }
            if (type == PieceType.ROOK || type == PieceType.CANNON) {
                if (sameFileOrRank(source, king)) {
                    int between = board.countBetween(source, king);
                    if (type == PieceType.ROOK && between <= 2) {
                        rookLike = true;
                        score += between <= 1 ? 1_050 : 420;
                    } else if (type == PieceType.CANNON && between <= 3) {
                        cannonLike = true;
                        score += between == 1 ? 1_250 : between == 2 ? 620 : 260;
                    }
                }
                if (attackerKing != null && sameFileOrRank(attackerKing, source)
                        && board.countBetween(attackerKing, source) == 0
                        && (isHomeSide(source, defender) || boardControlsPalace(board, source, defender))) {
                    kingBacked = true;
                    score += 760;
                }
            } else if (type == PieceType.PAWN && isHomeSide(source, defender)) {
                int palaceDistance = distanceToPalace(source, defender);
                if (palaceDistance <= 2 || distance <= 3) {
                    pawnNet = true;
                    score += 720 + Math.max(0, 4 - palaceDistance) * 220;
                }
            } else if (type == PieceType.KNIGHT && distance <= 4) {
                score += Math.max(0, 5 - distance) * 240;
            }
        }

        score += palacePressure;
        if (nearbyAttackers >= 3) {
            score += 1_200;
        } else if (nearbyAttackers == 2) {
            score += 560;
        }
        if (rookLike && cannonLike) {
            score += OPPONENT_PLAN_THREAT_BONUS;
        }
        if (pawnNet && (rookLike || cannonLike)) {
            score += 1_150;
        }
        if (kingBacked && (rookLike || cannonLike || pawnNet)) {
            score += 950;
        }

        int result = Math.min(12_000, Math.max(0, score));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private boolean threatensImportantPiece(Board board, Position attackerSource, PlayerColor attacker, PlayerColor defender) {
        for (Position target : board.occupiedPositions()) {
            Piece piece = board.get(target);
            if (piece == null || piece.color() != defender || knownType(piece) == PieceType.KING) {
                continue;
            }
            if (pieceSearchValue(board, piece, target) >= HIGH_VALUE_PIECE
                    && ruleEngine.canMove(board, attackerSource, target, attacker)) {
                return true;
            }
        }
        return false;
    }

    private int invadingPieceScore(Board board, PlayerColor color) {
        int score = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color.opponent()) {
                continue;
            }
            int danger = homeInvaderDanger(board, position, color);
            if (danger <= 0) {
                continue;
            }
            score += danger;
            if (attackersValue(board, position, color) > 0) {
                score += HOME_INVADER_PENALTY / 2;
            }
        }
        return Math.min(14_000, score);
    }

    private int homeInvaderDanger(Board board, Position position, PlayerColor defender) {
        Piece piece = board.get(position);
        if (piece == null || piece.color() == defender || !isHomeSide(position, defender)) {
            return 0;
        }
        PieceType type = knownType(piece);
        Position king = board.findKing(defender);
        int distance = king == null ? 9 : manhattan(position, king);
        int value = pieceSearchValue(board, piece, position);
        int score = 0;
        if (value >= HIGH_VALUE_PIECE) {
            score += INVADING_MAJOR_PENALTY + value;
        } else if (type == PieceType.PAWN) {
            score += HOME_INVADER_PENALTY + Math.max(0, 7 - distance) * 260;
            score += advancingPawnEndgameDanger(board, position, defender);
        } else {
            score += HOME_INVADER_PENALTY / 2 + Math.max(0, 6 - distance) * 140;
        }
        if (king != null && sameFileOrRank(position, king)) {
            score += 650;
        }
        if (ownPalace(position, defender)) {
            score += 1_200;
        }
        return score;
    }

    private int advancingPawnEndgameDanger(Board board, Position pawnPosition, PlayerColor defender) {
        Piece pawn = board.get(pawnPosition);
        if (pawn == null || pawn.color() == defender || knownType(pawn) != PieceType.PAWN) {
            return 0;
        }
        Position king = board.findKing(defender);
        if (king == null) {
            return WIN_SCORE / 5;
        }
        int distance = manhattan(pawnPosition, king);
        int palaceDistance = distanceToPalace(pawnPosition, defender);
        int score = Math.max(0, 6 - distance) * 180 + Math.max(0, 4 - palaceDistance) * 260;
        int nextY = pawnPosition.y() + pawn.color().forwardDirection();
        if (Position.isInside(pawnPosition.x(), nextY)) {
            Position next = new Position(pawnPosition.x(), nextY);
            if (board.get(next) == null && isHomeSide(next, defender)) {
                score += 420;
            }
            if (ownPalace(next, defender)) {
                score += 900;
            }
        }
        if (attackersValue(board, pawnPosition, defender) == 0) {
            score += 350;
        }
        return score;
    }

    private int pawnConstrictionProgressScore(Board before, Board after, Move move, PlayerColor color) {
        int progress = pawnConstrictionScore(after, color) - pawnConstrictionScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(PAWN_CONSTRICTION_PROGRESS_BONUS, progress);
    }

    private int pawnConstrictionScore(Board board, PlayerColor color) {
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        int majorPressure = majorKingAttackScore(board, color);
        for (Position pawnPosition : board.occupiedPositions()) {
            Piece pawn = board.get(pawnPosition);
            if (pawn == null || pawn.color() != color || knownType(pawn) != PieceType.PAWN
                    || !isOpponentSide(pawnPosition, color)) {
                continue;
            }
            int distance = manhattan(pawnPosition, enemyKing);
            int rank = forwardRank(pawnPosition, color);
            score += rank * 55 + Math.max(0, 7 - distance) * 120;
            if (ownPalace(pawnPosition, color.opponent())) {
                score += 980;
            } else if (distance <= 2) {
                score += 520;
            }
            if (Math.abs(pawnPosition.x() - enemyKing.x()) <= 1) {
                score += 420;
            }
            Position forward = pawnForwardTarget(pawnPosition, color);
            if (forward != null && ownPalace(forward, color.opponent())) {
                score += 520;
            }
            if (defendersValue(board, pawnPosition, color) > 0) {
                score += 260;
            }
            if (connectedPawnCount(board, pawnPosition, color) > 0) {
                score += 220;
            }
            if (majorPressure >= 700) {
                score += 360;
            }
        }
        return Math.min(3_800, score);
    }

    private int distanceToPalace(Position position, PlayerColor palaceOwner) {
        int xDistance = position.x() < 3 ? 3 - position.x() : Math.max(0, position.x() - 5);
        int yDistance;
        if (palaceOwner == PlayerColor.RED) {
            yDistance = position.y() < 0 ? -position.y() : Math.max(0, position.y() - 2);
        } else {
            yDistance = position.y() < 7 ? 7 - position.y() : Math.max(0, position.y() - 9);
        }
        return xDistance + yDistance;
    }

    private int exposedMovePenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece moved = after.get(move.destination());
        if (moved == null || moved.type() == PieceType.KING) {
            return 0;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        Piece captured = before.get(move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int attackers = attackersValue(after, move.destination(), color.opponent());
        if (attackers == 0) {
            return 0;
        }
        int opponentExchangeGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        if (opponentExchangeGain <= capturedValue) {
            return 0;
        }
        int defenders = defendersValue(after, move.destination(), color);
        int basePenalty = movedValue >= HIGH_VALUE_PIECE
                ? EXPOSED_MAJOR_PIECE_PENALTY
                : moved.type() == PieceType.PAWN ? 280 : EXPOSED_NON_PAWN_PENALTY;
        int penalty = basePenalty + opponentExchangeGain * 4 - capturedValue * 2;
        if (defenders > 0) {
            penalty = penalty * 2 / 3;
        }
        if (capturedValue >= movedValue) {
            penalty /= 3;
        }
        penalty -= sacrificeCompensation(before, after, move, color, movedValue, capturedValue);
        return Math.max(0, penalty);
    }

    private int nextMoveMajorLossPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece moved = after.get(move.destination());
        if (moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int opponentExchangeGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        if (opponentExchangeGain == 0) {
            return 0;
        }

        int exchangeLoss = opponentExchangeGain - capturedValue;
        int penalty = Math.max(0, exchangeLoss) * 7;
        if (movedValue >= HIGH_VALUE_PIECE) {
            penalty += NEXT_MOVE_MAJOR_LOSS_PENALTY;
        } else {
            penalty += Math.max(300, movedValue * 2);
        }
        if (capturedValue >= movedValue) {
            penalty /= 3;
        }
        if (defendersValue(after, move.destination(), color) > 0) {
            penalty = penalty * 2 / 3;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent())) {
            penalty += NON_DECISIVE_CHECK_PENALTY;
        }
        return Math.max(0, penalty);
    }

    private int badTradeHierarchyPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int moverTier = pieceTradeTier(knownType(mover));
        int capturedTier = captured == null ? 0 : pieceTradeTier(knownType(captured));
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int recaptureGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        boolean canBeTaken = recaptureGain > 0 || directReplyCaptureLoss(after, move, color, movedValue) > 0;
        if (!canBeTaken) {
            return 0;
        }

        int compensation = capturedValue;
        if (moverTier <= capturedTier && capturedValue + 120 >= movedValue) {
            return 0;
        }
        if (compensation >= movedValue) {
            return 0;
        }

        int tierGap = Math.max(1, moverTier - Math.max(1, capturedTier));
        int penalty = BAD_TRADE_HIERARCHY_PENALTY
                + tierGap * 1_600
                + Math.max(0, movedValue - compensation) * 5;
        if (captured == null && ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent())) {
            penalty += NON_DECISIVE_CHECK_PENALTY * 2;
        }
        if (knownType(mover) == PieceType.ROOK && capturedTier < pieceTradeTier(PieceType.ROOK)) {
            penalty += 2_000;
        } else if (knownType(mover) == PieceType.CANNON && capturedTier < pieceTradeTier(PieceType.CANNON)) {
            penalty += 1_100;
        }
        return Math.min(24_000, penalty);
    }

    private int unsoundMajorTradeCheckPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        Piece captured = before.get(move.destination());
        if (mover == null || moved == null || captured == null
                || captured.color() != color.opponent() || knownType(moved) == PieceType.KING) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        PieceType moverType = knownType(mover);
        PieceType capturedType = knownType(captured);
        int moverTier = pieceTradeTier(moverType);
        int capturedTier = pieceTradeTier(capturedType);
        if (moverTier <= capturedTier || moverTier < pieceTradeTier(PieceType.CANNON)) {
            return 0;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captureValue(before, captured, move.destination());
        int recaptureLoss = directReplyCaptureLoss(after, move, color, movedValue);
        int exchangeGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        int expectedLoss = Math.max(recaptureLoss, Math.max(0, exchangeGain - capturedValue));
        if (expectedLoss <= Math.max(120, movedValue / 4)) {
            return 0;
        }

        int mateNetGain = endgameMateNetScore(after, color) - endgameMateNetScore(before, color);
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int forcedCompensation = forcedSacrificeCompensation(before, after, move, color, movedValue, capturedValue);
        boolean escapableCheck = ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent());
        if (escapableCheck) {
            forcedCompensation = Math.min(forcedCompensation, capturedValue / 2);
            threatGain = 0;
            mateNetGain = Math.min(mateNetGain, 300);
        }
        if (!escapableCheck && (mateNetGain >= 1_200 || threatGain >= movedValue * 2
                || forcedCompensation >= expectedLoss + movedValue / 2)) {
            return 0;
        }

        int tierGap = moverTier - capturedTier;
        int penalty = UNSOUND_MAJOR_TRADE_CHECK_PENALTY
                + tierGap * 2_200
                + Math.max(0, movedValue - capturedValue) * 7
                + expectedLoss * 4;
        if (escapableCheck) {
            penalty += NON_DECISIVE_CHECK_PENALTY * 2;
        }
        if (moverType == PieceType.ROOK) {
            penalty += 2_800;
        } else if (moverType == PieceType.CANNON) {
            penalty += 1_500;
        }
        return Math.min(32_000, Math.max(0, penalty - forcedCompensation));
    }

    private int continuationTradeLossPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece moved = after.get(move.destination());
        if (moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int worst = 0;
        int movedValue = pieceSearchValue(after, moved, move.destination());
        for (Move opponentCapture : moveGenerator.generateActions(after, color.opponent(), 0)) {
            if (!opponentCapture.destination().equals(move.destination())
                    || !ruleEngine.canMoveAndKeepKingSafe(after, opponentCapture.source(), opponentCapture.destination(), color.opponent())) {
                continue;
            }
            Piece opponentAttacker = after.get(opponentCapture.source());
            Board afterOpponentCapture = applyForSearch(after, opponentCapture);
            int opponentAttackerValue = opponentAttacker == null ? 0
                    : pieceSearchValue(after, opponentAttacker, opponentCapture.source());
            int baseLoss = Math.max(0, movedValue - opponentAttackerValue);
            for (Move recapture : moveGenerator.generateActions(afterOpponentCapture, color, 0)) {
                if (!recapture.destination().equals(move.destination())
                        || !ruleEngine.canMoveAndKeepKingSafe(afterOpponentCapture, recapture.source(), recapture.destination(), color)) {
                    continue;
                }
                Piece recapturer = afterOpponentCapture.get(recapture.source());
                Board afterRecapture = applyForSearch(afterOpponentCapture, recapture);
                int recapturerValue = recapturer == null ? 0
                        : pieceSearchValue(afterOpponentCapture, recapturer, recapture.source());
                int nextOpponentGain = exchangeSequenceGain(afterRecapture, move.destination(), color.opponent());
                int chainLoss = baseLoss + Math.max(0, nextOpponentGain - opponentAttackerValue / 2);
                if (pieceTradeTier(knownType(moved)) > pieceTradeTier(PieceType.PAWN)
                        && nextOpponentGain >= recapturerValue / 2) {
                    chainLoss += CONTINUATION_TRADE_LOSS_PENALTY;
                }
                worst = Math.max(worst, chainLoss);
            }
            if (baseLoss > 0 && opponentAttackerValue <= value(PieceType.PAWN)) {
                worst = Math.max(worst, baseLoss + CONTINUATION_TRADE_LOSS_PENALTY / 2);
            }
        }
        Piece captured = before.get(move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int planGain = Math.max(0, opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color))
                + Math.max(0, coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color)) / 2;
        return Math.max(0, worst - capturedValue - planGain);
    }

    private int hiddenRecaptureUncertainTradePenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        Piece captured = before.get(move.destination());
        if (mover == null || moved == null || captured == null
                || captured.color() != color.opponent() || knownType(moved) == PieceType.KING) {
            return 0;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captureValue(before, captured, move.destination());
        if (capturedValue >= movedValue || movedValue < value(PieceType.KNIGHT)) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }

        int worstPenalty = 0;
        for (Move reply : moveGenerator.generateActions(after, color.opponent(), 0)) {
            if (!reply.destination().equals(move.destination())
                    || !ruleEngine.canMoveAndKeepKingSafe(after, reply.source(), reply.destination(), color.opponent())) {
                continue;
            }
            Piece hiddenAttacker = after.get(reply.source());
            if (hiddenAttacker == null || hiddenAttacker.visible()) {
                continue;
            }
            int hiddenExpected = expectedHiddenValue(before, color.opponent(), reply.source());
            int hiddenLikelyHigh = likelyHighHiddenValue(before, color.opponent(), reply.source());
            Board afterReply = applyForSearch(after, reply);
            int forcedRecapture = forcedRecaptureValue(afterReply, move.destination(), color);
            int materialCompensation = capturedValue + Math.min(hiddenExpected, forcedRecapture);
            int uncertaintyGap = Math.max(0, movedValue - hiddenExpected);
            int likelyGap = Math.max(0, movedValue - hiddenLikelyHigh);
            int netLoss = movedValue - materialCompensation;

            int penalty = HIDDEN_RECAPTURE_UNCERTAIN_TRADE_PENALTY
                    + Math.max(0, netLoss) * 4
                    + uncertaintyGap * 3
                    + likelyGap * 2;
            if (capturedValue <= value(PieceType.PAWN)) {
                penalty += 1_400;
            }
            if (knownType(mover) == PieceType.ROOK || knownType(mover) == PieceType.CANNON) {
                penalty += 1_100;
            }
            if (forcedRecapture >= hiddenExpected + 180 && hiddenExpected >= movedValue) {
                penalty /= 3;
            } else if (forcedRecapture >= hiddenExpected) {
                penalty = penalty * 2 / 3;
            }
            int planGain = Math.max(0, candidatePlanScore(after, color) - candidatePlanScore(before, color))
                    + Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color));
            penalty -= planGain / 2;
            worstPenalty = Math.max(worstPenalty, penalty);
        }
        return Math.max(0, Math.min(18_000, worstPenalty));
    }

    private int darkUncertainRecaptureRiskPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        Piece captured = before.get(move.destination());
        if (mover == null || moved == null || captured == null
                || captured.color() != color.opponent() || mover.visible() || captured.visible()
                || knownType(moved) == PieceType.KING) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        PieceType oneShotType = knownType(mover);
        if (oneShotType != PieceType.ROOK && oneShotType != PieceType.CANNON && oneShotType != PieceType.KNIGHT) {
            return 0;
        }

        int ownLikelyHigh = likelyHighHiddenValue(before, color, move.source());
        int capturedExpected = expectedHiddenValue(before, color.opponent(), move.destination());
        int capturedLikelyHigh = likelyHighHiddenValue(before, color.opponent(), move.destination());
        boolean ownCoreMayRemain = remainingCoreHiddenCount(before, color) > 0;
        if (!ownCoreMayRemain && ownLikelyHigh < value(PieceType.CANNON)) {
            return 0;
        }

        int worst = 0;
        for (Move reply : moveGenerator.generateActions(after, color.opponent(), 0)) {
            if (!reply.destination().equals(move.destination())
                    || !ruleEngine.canMoveAndKeepKingSafe(after, reply.source(), reply.destination(), color.opponent())) {
                continue;
            }
            Piece recapturer = after.get(reply.source());
            if (recapturer == null) {
                continue;
            }
            Board afterReply = applyForSearch(after, reply);
            int forcedRecapture = forcedRecaptureValue(afterReply, move.destination(), color);
            int compensation = capturedExpected + Math.min(forcedRecapture, capturedLikelyHigh / 2);
            int uncertaintyLoss = Math.max(0, ownLikelyHigh - compensation);
            int penalty = DARK_RECAPTURE_RISK_PENALTY
                    + uncertaintyLoss * 5
                    + hiddenValueSpread(before, color) / 2;
            if (!recapturer.visible()) {
                penalty += 2_200 + likelyHighHiddenValue(before, color.opponent(), reply.source()) / 2;
            }
            if (oneShotType == PieceType.ROOK) {
                penalty += 1_600;
            } else if (oneShotType == PieceType.CANNON) {
                penalty += 1_000;
            }
            if (knownType(captured) == PieceType.PAWN || knownType(captured) == PieceType.GUARD
                    || knownType(captured) == PieceType.BISHOP) {
                penalty += 900;
            }
            int planGain = Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color))
                    + Math.max(0, candidatePlanScore(after, color) - candidatePlanScore(before, color));
            penalty -= planGain / 3;
            if (forcedRecapture >= ownLikelyHigh) {
                penalty /= 2;
            }
            worst = Math.max(worst, penalty);
        }
        return Math.max(0, Math.min(22_000, worst));
    }

    private int majorSafetyNeglectPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (ruleEngine.isInCheck(before, color)) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int beforeRisk = threatenedMajorSafetyScore(before, color);
        if (beforeRisk < MAJOR_SAFETY_PRIORITY_PENALTY / 2) {
            return 0;
        }
        int afterRisk = threatenedMajorSafetyScore(after, color);
        int riskDrop = beforeRisk - afterRisk;
        Piece captured = before.get(move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        if (riskDrop >= Math.min(900, beforeRisk / 2) || capturedValue >= HIGH_VALUE_PIECE) {
            return 0;
        }
        Piece mover = before.get(move.source());
        int penalty = MAJOR_SAFETY_PRIORITY_PENALTY + Math.max(0, beforeRisk - riskDrop);
        if (mover != null && !mover.visible()) {
            penalty += REVEAL_BONUS + 1_200;
        }
        if (captured == null) {
            penalty += 900;
        }
        return Math.min(18_000, penalty);
    }

    private int illusoryMajorThreatPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int bestThreatenedMajor = bestNewMajorThreatValue(before, after, move, color);
        if (bestThreatenedMajor < value(PieceType.KNIGHT)) {
            return 0;
        }
        if (capturedValue >= bestThreatenedMajor) {
            return 0;
        }
        if (!attackedEnemyMajorCanEscape(after, color)) {
            return 0;
        }
        int ownCost = exposedMovePenalty(before, after, move, color)
                + Math.max(0, threatenedMajorSafetyScore(after, color) - threatenedMajorSafetyScore(before, color));
        if (legalAttackersValue(after, move.destination(), color.opponent()) > 0) {
            ownCost += pieceSearchValue(after, moved, move.destination()) / 2;
        }
        if (knownType(moved) == PieceType.PAWN || knownType(moved) == PieceType.GUARD || knownType(moved) == PieceType.BISHOP) {
            ownCost += 700;
        }
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int pressureGain = coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color);
        if (threatGain >= 1_400 || pressureGain >= 800) {
            ownCost /= 2;
        }
        if (ownCost < 700) {
            return 0;
        }
        return Math.min(12_000, ILLUSORY_MAJOR_THREAT_PENALTY + ownCost + bestThreatenedMajor / 2 - capturedValue);
    }

    private int pointlessRepositionPenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || !mover.visible() || knownType(mover) == PieceType.KING) {
            return 0;
        }
        if (before.get(move.destination()) != null) {
            return 0;
        }
        if (ruleEngine.isInCheck(before, color)
                || (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent()))) {
            return 0;
        }
        boolean escapedAttack = legalAttackersValue(before, move.source(), color.opponent()) > 0
                && legalAttackersValue(after, move.destination(), color.opponent()) == 0;
        boolean nonDecisiveCheck = ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent());
        if (escapedAttack && !nonDecisiveCheck
                && threatenedMajorSafetyScore(before, color) > threatenedMajorSafetyScore(after, color)) {
            return 0;
        }
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        int pressureGain = coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color);
        int planGain = strategicPlanPotential(after, color) - strategicPlanPotential(before, color);
        int shapeGain = jieqiShapePressure(after, color) - jieqiShapePressure(before, color);
        int roleGain = pieceRoleScore(after, color) - pieceRoleScore(before, color);
        int screenGain = knownType(mover) == PieceType.CANNON
                ? cannonScreenScore(after, color) - cannonScreenScore(before, color)
                : 0;
        int unlockGain = knownType(mover) == PieceType.CANNON
                ? cannonUnlockProgressScore(before, after, move, color)
                : 0;
        boolean enemyMajorCanEscape = attackedEnemyMajorCanEscape(after, color);
        if (!nonDecisiveCheck
                && (pressureGain >= 620 || planGain >= 620
                || shapeGain >= 360 || roleGain >= 520 || screenGain >= 320 || unlockGain >= CANNON_UNLOCK_BONUS
                || (threatGain >= 1_000 && (pressureGain >= 360 || planGain >= 360 || !enemyMajorCanEscape)))) {
            return 0;
        }
        if (nonDecisiveCheck && threatGain >= 2_600 && pressureGain >= 1_300) {
            return 0;
        }
        if (bestNewMajorThreatValue(before, after, move, color) >= value(PieceType.CANNON)
                && !enemyMajorCanEscape) {
            return 0;
        }
        int penalty = POINTLESS_REPOSITION_PENALTY + pieceSearchValue(before, mover, move.source()) / 2;
        PieceType moverType = knownType(mover);
        if (moverType == PieceType.PAWN || moverType == PieceType.GUARD || moverType == PieceType.BISHOP) {
            penalty = POINTLESS_REPOSITION_PENALTY / 2 + pieceSearchValue(before, mover, move.source());
        }
        if (moverType == PieceType.PAWN
                && (pawnConstrictionProgressScore(before, after, move, color) >= PAWN_CONSTRICTION_PROGRESS_BONUS / 2
                || pawnTrapProgressScore(before, after, move, color) >= PAWN_TRAP_BONUS / 2)) {
            return 0;
        }
        if (nonDecisiveCheck) {
            penalty += NON_DECISIVE_CHECK_PENALTY;
        }
        if (hasUsefulAlternativePieceMove(before, color, move.source())) {
            penalty += 1_400;
        }
        if (sameFileOrRank(move.source(), move.destination())
                && manhattan(move.source(), move.destination()) <= 2) {
            penalty += 600;
        }
        if (legalAttackersValue(after, move.destination(), color.opponent()) > 0) {
            penalty += pieceSearchValue(after, moved, move.destination());
        }
        return Math.min(11_000, penalty);
    }

    private boolean hasUsefulAlternativePieceMove(Board board, PlayerColor color, Position excludedSource) {
        int baseThreat = immediateThreatScore(board, color);
        int baseAttack = coordinatedKingAttackScore(board, color);
        int basePlan = strategicPlanPotential(board, color);
        int searched = 0;
        for (Move candidate : moveGenerator.generateActions(board, color, 0)) {
            if (candidate.source().equals(excludedSource) || searched++ >= 36) {
                continue;
            }
            Piece mover = board.get(candidate.source());
            if (mover == null || knownType(mover) == PieceType.KING) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, candidate.source(), candidate.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, candidate);
            Piece captured = board.get(candidate.destination());
            if (captured != null && captured.color() == color.opponent()
                    && captureValue(board, captured, candidate.destination()) >= value(PieceType.PAWN)) {
                return true;
            }
            if (immediateThreatScore(next, color) >= baseThreat + 700) {
                return true;
            }
            if (coordinatedKingAttackScore(next, color) >= baseAttack + 420) {
                return true;
            }
            if (legalAttackersValue(board, candidate.source(), color.opponent()) > 0
                    && legalAttackersValue(next, candidate.destination(), color.opponent()) == 0) {
                return true;
            }
            if (isOpponentSide(candidate.destination(), color)
                    && strategicPlanPotential(next, color) >= basePlan + 360) {
                return true;
            }
        }
        return false;
    }

    private int threatenedMajorSafetyScore(Board board, PlayerColor color) {
        int score = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color || !isMajorPieceForSafety(piece)) {
                continue;
            }
            int attackerValue = legalAttackersValue(board, position, color.opponent());
            if (attackerValue == 0) {
                continue;
            }
            int defenders = defendersValue(board, position, color);
            int pieceValue = pieceSearchValue(board, piece, position);
            int risk = pieceValue * 2 + MAJOR_SAFETY_PRIORITY_PENALTY / 2;
            if (defenders == 0) {
                risk += pieceValue + 1_200;
            } else if (attackerValue < pieceValue) {
                risk += pieceValue / 2;
            }
            if (!piece.visible()) {
                risk += HIDDEN_ONE_SHOT_PRESERVE_BONUS / 2;
            }
            score += risk;
        }
        return Math.min(24_000, score);
    }

    private boolean isMajorPieceForSafety(Piece piece) {
        PieceType type = knownType(piece);
        return type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT;
    }

    private int bestNewMajorThreatValue(Board before, Board after, Move move, PlayerColor color) {
        int best = 0;
        for (Position target : after.occupiedPositions()) {
            Piece enemy = after.get(target);
            if (enemy == null || enemy.color() != color.opponent() || !isMajorPieceForSafety(enemy)) {
                continue;
            }
            if (!ruleEngine.canMove(after, move.destination(), target, color)) {
                continue;
            }
            boolean alreadyThreatened = ruleEngine.canMove(before, move.source(), target, color);
            if (alreadyThreatened && before.get(move.destination()) == null) {
                continue;
            }
            best = Math.max(best, pieceSearchValue(after, enemy, target));
        }
        return best;
    }

    private boolean attackedEnemyMajorCanEscape(Board board, PlayerColor attackerColor) {
        PlayerColor defender = attackerColor.opponent();
        for (Position target : board.occupiedPositions()) {
            Piece piece = board.get(target);
            if (piece == null || piece.color() != defender || !isMajorPieceForSafety(piece)) {
                continue;
            }
            if (legalAttackersValue(board, target, attackerColor) == 0) {
                continue;
            }
            for (Move reply : moveGenerator.generateActions(board, defender, 0)) {
                if (!reply.source().equals(target)
                        || !ruleEngine.canMoveAndKeepKingSafe(board, reply.source(), reply.destination(), defender)) {
                    continue;
                }
                Board escaped = applyForSearch(board, reply);
                if (legalAttackersValue(escaped, reply.destination(), attackerColor) == 0) {
                    return true;
                }
                Piece captured = board.get(reply.destination());
                if (captured != null && captured.color() == attackerColor
                        && captureValue(board, captured, reply.destination()) >= value(PieceType.KNIGHT)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int urgentTradePenalty(Board before, Board after, Move move, PlayerColor color, boolean emergencyRelief) {
        int penalty = badTradeHierarchyPenalty(before, after, move, color)
                + continuationTradeLossPenalty(before, after, move, color);
        if (penalty == 0) {
            return 0;
        }
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int defenseRelief = Math.max(0, kingDangerScore(before, color) - kingDangerScore(after, color))
                + Math.max(0, opponentPlanThreatScore(before, color) - opponentPlanThreatScore(after, color));
        int relief = defenseRelief;
        if (emergencyRelief) {
            relief += 2_400;
        }
        int adjusted = Math.max(0, penalty - relief);
        Piece mover = before.get(move.source());
        Piece captured = before.get(move.destination());
        Piece moved = after.get(move.destination());
        if (mover != null && moved != null && captured != null
                && pieceTradeTier(knownType(mover)) > pieceTradeTier(knownType(captured))
                && exchangeSequenceGain(after, move.destination(), color.opponent()) > 0
                && !emergencyRelief) {
            adjusted = Math.max(adjusted, BAD_TRADE_HIERARCHY_PENALTY);
        }
        if (mover != null && moved != null && captured == null
                && ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent())
                && exchangeSequenceGain(after, move.destination(), color.opponent()) > 0
                && !emergencyRelief) {
            adjusted = Math.max(adjusted, BAD_TRADE_HIERARCHY_PENALTY + NON_DECISIVE_CHECK_PENALTY);
        }
        return adjusted;
    }

    private int pieceTradeTier(PieceType type) {
        return switch (type) {
            case KING -> 99;
            case ROOK -> 4;
            case CANNON -> 3;
            case KNIGHT -> 2;
            case PAWN, GUARD, BISHOP -> 1;
        };
    }

    private int uncertainRecapturePenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        Piece captured = before.get(move.destination());
        if (mover == null || moved == null || captured == null
                || captured.color() != color.opponent() || knownType(captured) == PieceType.KING) {
            return 0;
        }
        boolean uncertainExchange = !mover.visible() || !captured.visible();
        if (!uncertainExchange) {
            return 0;
        }

        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captureValue(before, captured, move.destination());
        int directLoss = directReplyCaptureLoss(after, move, color, movedValue);
        if (directLoss <= Math.max(80, capturedValue / 4)) {
            return 0;
        }

        int planGain = Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color))
                + Math.max(0, kingDangerScore(after, color.opponent()) - kingDangerScore(before, color.opponent())) / 2;
        if (ruleEngine.isInCheck(after, color.opponent())) {
            planGain += moveGenerator.hasCheckEscape(after, color.opponent()) ? CHECK_BONUS / 3 : WIN_SCORE / 3;
        }
        int forcedValue = forcedRecaptureValue(after, move.destination(), color);
        int compensation = capturedValue + forcedValue + planGain / 3;
        int netRisk = directLoss - compensation;
        boolean equalUnknownTrade = !captured.visible()
                && directLoss + 160 >= capturedValue
                && planGain < 900
                && forcedValue < directLoss;
        if (netRisk <= 120 && !equalUnknownTrade) {
            return 0;
        }

        int penalty = UNCERTAIN_RECAPTURE_PENALTY + Math.max(0, netRisk) * 5;
        if (equalUnknownTrade) {
            penalty += 1_600;
        }
        if (!captured.visible()) {
            penalty += 1_000;
        }
        if (!mover.visible()) {
            penalty += 700;
        }
        if (knownType(mover) == PieceType.CANNON || knownType(mover) == PieceType.ROOK) {
            penalty += 550;
        }
        if (!mover.visible() && !captured.visible()
                && (knownType(mover) == PieceType.ROOK || knownType(mover) == PieceType.CANNON)
                && directLoss > 0
                && forcedValue < directLoss) {
            penalty += DARK_UNCERTAIN_EXCHANGE_EXTRA_PENALTY;
            if (captureValue(before, captured, move.destination()) < value(PieceType.CANNON)) {
                penalty += 900;
            }
        }
        if (capturedValue >= movedValue + 220) {
            penalty /= 2;
        }
        if (forcedValue >= directLoss) {
            penalty /= 2;
        }
        return Math.max(0, penalty);
    }

    private int hiddenOneShotWastePenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || mover.visible()) {
            return 0;
        }
        PieceType oneShotType = knownType(mover);
        if (oneShotType != PieceType.ROOK && oneShotType != PieceType.CANNON && oneShotType != PieceType.KNIGHT) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        boolean usefulCashIn = captured != null && captured.color() == color.opponent()
                && captureValue(before, captured, move.destination()) >= HIGH_VALUE_PIECE;
        Board next = after;
        if (ruleEngine.isInCheck(next, color.opponent())
                || opponentPlanThreatScore(before, color) - opponentPlanThreatScore(next, color) >= 900
                || usefulCashIn) {
            return 0;
        }

        int beforeUtility = hiddenOneShotUtility(before, move.source(), color);
        if (beforeUtility <= 0) {
            return 0;
        }
        int afterUtility = hiddenOneShotUtility(after, move.destination(), color);
        int lostUtility = beforeUtility - afterUtility;
        if (lostUtility <= 0) {
            return 0;
        }
        int penalty = lostUtility + HIDDEN_ONE_SHOT_PRESERVE_BONUS;
        if (oneShotType == PieceType.ROOK) {
            penalty += 900;
        } else if (oneShotType == PieceType.CANNON) {
            penalty += 520;
        }
        return Math.min(5_500, penalty);
    }

    private int hiddenOneShotUtility(Board board, Position source, PlayerColor color) {
        Piece piece = board.get(source);
        if (piece == null || piece.color() != color || piece.visible()) {
            return 0;
        }
        PieceType oneShotType = knownType(piece);
        if (oneShotType != PieceType.ROOK && oneShotType != PieceType.CANNON && oneShotType != PieceType.KNIGHT) {
            return 0;
        }
        int score = 0;
        Position enemyKing = board.findKing(color.opponent());
        for (Position target : board.occupiedPositions()) {
            if (target.equals(source)) {
                continue;
            }
            Piece targetPiece = board.get(target);
            if (targetPiece == null) {
                continue;
            }
            if (targetPiece.color() == color && knownType(targetPiece) != PieceType.KING
                    && ruleEngine.canMove(board, source, target, color)) {
                int protectedValue = pieceSearchValue(board, targetPiece, target);
                score += Math.min(900, protectedValue);
                if (attackersValue(board, target, color.opponent()) > 0) {
                    score += 900;
                }
            } else if (targetPiece.color() == color.opponent()
                    && ruleEngine.canMove(board, source, target, color)) {
                score += captureValue(board, targetPiece, target) / 2;
            }
        }
        if (enemyKing != null && ruleEngine.canMove(board, source, enemyKing, color)) {
            score += 1_250;
        }
        return Math.min(3_200, score);
    }

    private int hiddenWorstCaseRiskPenalty(Board before, Board after, Move move, PlayerColor color) {
        int hiddenPhase = hiddenPhase(before);
        if (hiddenPhase < 18) {
            return 0;
        }
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int penalty = 0;
        Position ownKing = before.findKing(color);
        if (captured != null && captured.color() == color.opponent() && !captured.visible()) {
            int expected = expectedHiddenValue(before, color.opponent(), move.destination());
            int worstReasonable = likelyHighHiddenValue(before, color.opponent(), move.destination());
            int uncertainty = Math.max(0, worstReasonable - expected);
            int opponentGain = exchangeSequenceGain(after, move.destination(), color.opponent());
            if (opponentGain > 0 && forcedRecaptureValue(after, move.destination(), color) < opponentGain) {
                penalty += uncertainty + opponentGain;
            }
            if (ownKing != null && manhattan(move.destination(), ownKing) <= 4
                    && capturedValueCouldBeMajor(before, color.opponent())) {
                penalty += 520;
            }
        }
        for (Position position : after.occupiedPositions()) {
            Piece piece = after.get(position);
            if (piece == null || piece.color() != color.opponent() || piece.visible()) {
                continue;
            }
            int danger = hiddenInvaderInformationDanger(after, position, color);
            if (danger <= 0) {
                continue;
            }
            if (ownKing != null && manhattan(position, ownKing) <= 3) {
                penalty += danger / 2;
            } else if (isHomeSide(position, color)) {
                penalty += danger / 4;
            }
        }
        if (!mover.visible() && attackersValue(after, move.destination(), color.opponent()) > 0
                && defendersValue(after, move.destination(), color) == 0) {
            penalty += likelyHighHiddenValue(before, color, move.destination()) / 2;
        }
        return Math.min(HIDDEN_WORST_CASE_PENALTY + hiddenPhase * 28, Math.max(0, penalty));
    }

    private int likelyHighHiddenValue(Board board, PlayerColor color, Position position) {
        Map<PieceType, Integer> counts = remainingHiddenTypeCounts(board, color);
        int best = expectedHiddenValue(board, color, position);
        for (PieceType type : new PieceType[]{PieceType.ROOK, PieceType.CANNON, PieceType.KNIGHT}) {
            if (counts.getOrDefault(type, 0) > 0) {
                best = Math.max(best, expectedTypeValue(color, type, position));
            }
        }
        return best;
    }

    private boolean capturedValueCouldBeMajor(Board board, PlayerColor color) {
        Map<PieceType, Integer> counts = remainingHiddenTypeCounts(board, color);
        return counts.getOrDefault(PieceType.ROOK, 0) > 0
                || counts.getOrDefault(PieceType.CANNON, 0) > 0
                || counts.getOrDefault(PieceType.KNIGHT, 0) > 0;
    }

    private int escapedCheckExposurePenalty(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(after, color.opponent())
                || !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        Piece moved = after.get(move.destination());
        if (moved == null || pieceSearchValue(after, moved, move.destination()) < HIGH_VALUE_PIECE) {
            return 0;
        }
        int worst = 0;
        int directLoss = directReplyCaptureLoss(after, move, color, pieceSearchValue(after, moved, move.destination()));
        if (directLoss > 0) {
            worst = Math.max(worst, NEXT_MOVE_MAJOR_LOSS_PENALTY + directLoss * 3);
        }
        for (Move reply : moveGenerator.generateActions(after, color.opponent(), 0)) {
            if (!ruleEngine.canMoveAndKeepKingSafe(after, reply.source(), reply.destination(), color.opponent())) {
                continue;
            }
            if (reply.destination().equals(move.destination())) {
                continue;
            }
            Board escaped = applyForSearch(after, reply);
            Piece stillThere = escaped.get(move.destination());
            if (stillThere == null || stillThere.color() != color) {
                continue;
            }
            int attackerValue = legalAttackersValue(escaped, move.destination(), color.opponent());
            if (attackerValue > 0) {
                worst = Math.max(worst, NEXT_MOVE_MAJOR_LOSS_PENALTY
                        + pieceSearchValue(escaped, stillThere, move.destination()) * 3);
            }
        }
        return worst;
    }

    private int unsoundCheckSacrificePenalty(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(after, color.opponent())
                || !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        Piece moved = after.get(move.destination());
        if (moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        PieceType movedType = knownType(moved);
        int movedValue = pieceSearchValue(after, moved, move.destination());
        Piece captured = before.get(move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int mateNetGain = endgameMateNetScore(after, color) - endgameMateNetScore(before, color);
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        if (capturedValue >= movedValue || mateNetGain >= 900 || threatGain >= movedValue * 2) {
            return 0;
        }
        int worstLoss = directReplyCaptureLoss(after, move, color, movedValue);
        for (Move reply : moveGenerator.generateActions(after, color.opponent(), 0)) {
            if (!ruleEngine.canMoveAndKeepKingSafe(after, reply.source(), reply.destination(), color.opponent())) {
                continue;
            }
            if (reply.destination().equals(move.destination())) {
                continue;
            }
            Board escaped = applyForSearch(after, reply);
            Piece stillThere = escaped.get(move.destination());
            if (stillThere == null || stillThere.color() != color) {
                continue;
            }
            int replyGain = exchangeSequenceGain(escaped, move.destination(), color.opponent());
            worstLoss = Math.max(worstLoss, replyGain);
        }
        int netLoss = worstLoss - capturedValue;
        if (netLoss <= Math.max(120, movedValue / 3)) {
            return 0;
        }
        int penalty = (movedValue >= HIGH_VALUE_PIECE ? UNSOUND_CHECK_SACRIFICE_PENALTY : 1_800)
                + netLoss * 7;
        if (capturedValue <= value(PieceType.PAWN)) {
            penalty += movedValue * 4;
        }
        if (movedType == PieceType.CANNON || movedType == PieceType.KNIGHT) {
            penalty += 900;
        }
        return penalty;
    }

    private int directReplyCaptureLoss(Board after, Move move, PlayerColor color, int movedValue) {
        int worstLoss = 0;
        for (Move reply : moveGenerator.generateActions(after, color.opponent(), 0)) {
            if (!reply.destination().equals(move.destination())
                    || !ruleEngine.canMoveAndKeepKingSafe(after, reply.source(), reply.destination(), color.opponent())) {
                continue;
            }
            Board escaped = applyForSearch(after, reply);
            int recapture = exchangeSequenceGain(escaped, move.destination(), color);
            worstLoss = Math.max(worstLoss, Math.max(0, movedValue - recapture));
        }
        return worstLoss;
    }

    private int uncompensatedSacrificePenalty(Board before, Board after, Move move, PlayerColor color) {
        Piece moved = after.get(move.destination());
        if (moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        int opponentExchangeGain = exchangeSequenceGain(after, move.destination(), color.opponent());
        if (opponentExchangeGain == 0) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        int forcedCompensation = forcedSacrificeCompensation(before, after, move, color, movedValue, capturedValue);
        int expectedLoss = Math.max(0, opponentExchangeGain - capturedValue);
        if (forcedCompensation >= expectedLoss + movedValue / 2) {
            return 0;
        }
        int penalty = UNCOMPENSATED_SACRIFICE_PENALTY + expectedLoss * 8;
        if (movedValue >= HIGH_VALUE_PIECE) {
            penalty += NEXT_MOVE_MAJOR_LOSS_PENALTY + movedValue * 3;
        } else if (knownType(moved) == PieceType.PAWN && pawnTrapScore(after, move.destination(), color) > PAWN_TRAP_BONUS) {
            penalty /= 3;
        }
        if (capturedValue >= movedValue) {
            penalty /= 2;
        }
        if (defendersValue(after, move.destination(), color) > 0
                && forcedCompensation >= expectedLoss) {
            penalty /= 2;
        }
        return Math.max(0, penalty - forcedCompensation * 2);
    }

    private int obviousGiftPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return 0;
        }
        int worstGift = bestOpponentProfitableCapture(after, color);
        if (worstGift <= 0) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int capturedValue = captured == null ? 0 : captureValue(before, captured, move.destination());
        Piece moved = after.get(move.destination());
        int movedValue = moved == null ? 0 : pieceSearchValue(after, moved, move.destination());
        int concreteCompensation = capturedValue * 2
                + forcedRecaptureValue(after, move.destination(), color)
                + Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color)) / 3;
        if (moved != null && knownType(moved) == PieceType.PAWN
                && pawnTrapScore(after, move.destination(), color) >= PAWN_TRAP_BONUS) {
            concreteCompensation += movedValue;
        }
        int netGift = worstGift - concreteCompensation;
        if (netGift <= 180) {
            return 0;
        }
        int penalty = OBVIOUS_GIFT_PENALTY + netGift * 5;
        if (worstGift >= HIGH_VALUE_PIECE) {
            penalty += NEXT_MOVE_MAJOR_LOSS_PENALTY;
        }
        return penalty;
    }

    private int bestOpponentProfitableCapture(Board board, PlayerColor color) {
        int worst = 0;
        for (Move reply : moveGenerator.generateActions(board, color.opponent(), 0)) {
            Piece victim = board.get(reply.destination());
            if (victim == null || victim.color() != color || knownType(victim) == PieceType.KING) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, reply.source(), reply.destination(), color.opponent())) {
                continue;
            }
            Piece attacker = board.get(reply.source());
            if (attacker == null) {
                continue;
            }
            int victimValue = pieceSearchValue(board, victim, reply.destination());
            Board afterCapture = applyForSearch(board, reply);
            int opponentContinuationLoss = exchangeSequenceGain(afterCapture, reply.destination(), color);
            int gain = victimValue - opponentContinuationLoss;
            if (victimValue >= HIGH_VALUE_PIECE && opponentContinuationLoss == 0) {
                gain += EXPOSED_MAJOR_PIECE_PENALTY;
            }
            if (ruleEngine.isInCheck(afterCapture, color)
                    && !moveGenerator.hasCheckEscape(afterCapture, color)) {
                gain += WIN_SCORE / 4;
            }
            if (gain > 0) {
                worst = Math.max(worst, gain);
            }
        }
        return worst;
    }

    private int forcedSacrificeCompensation(
            Board before,
            Board after,
            Move move,
            PlayerColor color,
            int movedValue,
            int capturedValue) {
        if (ruleEngine.isInCheck(after, color.opponent())
                && !moveGenerator.hasCheckEscape(after, color.opponent())) {
            return WIN_SCORE / 3;
        }
        int compensation = capturedValue;
        compensation += forcedRecaptureValue(after, move.destination(), color);
        int threatGain = immediateThreatScore(after, color) - immediateThreatScore(before, color);
        if (threatGain >= movedValue * 2) {
            compensation += threatGain / 2;
        }
        int trapScore = pawnTrapScore(after, move.destination(), color);
        if (trapScore >= PAWN_TRAP_BONUS) {
            compensation += trapScore;
        }
        compensation += baitTrapScore(after, move.destination(), color) / 2;
        int relief = invadingPieceScore(before, color) - invadingPieceScore(after, color);
        if (relief >= 1_200) {
            compensation += relief;
        }
        int unlock = cannonUnlockProgressScore(before, after, move, color);
        if (unlock >= CANNON_UNLOCK_BONUS) {
            compensation += unlock;
        }
        return compensation;
    }

    private int baitTrapScore(Board board, Position baitPosition, PlayerColor color) {
        Piece bait = board.get(baitPosition);
        if (bait == null || bait.color() != color || knownType(bait) == PieceType.KING) {
            return 0;
        }
        int baitValue = pieceSearchValue(board, bait, baitPosition);
        if (baitValue > HIGH_VALUE_PIECE && knownType(bait) != PieceType.PAWN) {
            return 0;
        }
        int enemyAttackers = attackersValue(board, baitPosition, color.opponent());
        int majorDefenders = majorDefenderCount(board, baitPosition, color);
        if (enemyAttackers == 0 || majorDefenders == 0) {
            return 0;
        }
        int score = 650 + majorDefenders * 320;
        if (knownType(bait) == PieceType.PAWN) {
            score += 420;
        }
        if (boardControlsPalace(board, baitPosition, color.opponent())) {
            score += 520;
        }
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing != null && manhattan(baitPosition, enemyKing) <= 4) {
            score += 260;
        }
        return Math.min(2_400, score);
    }

    private int forcedRecaptureValue(Board board, Position target, PlayerColor color) {
        int best = 0;
        for (Move reply : moveGenerator.generateActions(board, color.opponent(), 0)) {
            if (!reply.destination().equals(target)
                    || !ruleEngine.canMoveAndKeepKingSafe(board, reply.source(), reply.destination(), color.opponent())) {
                continue;
            }
            Board afterCapture = applyForSearch(board, reply);
            best = Math.max(best, exchangeSequenceGain(afterCapture, target, color));
        }
        return best;
    }

    private int sacrificeCompensation(
            Board before,
            Board after,
            Move move,
            PlayerColor color,
            int movedValue,
            int capturedValue) {
        int compensation = capturedValue * 2;
        if (ruleEngine.isInCheck(after, color.opponent())) {
            compensation += moveGenerator.hasCheckEscape(after, color.opponent())
                    ? CHECK_BONUS / 4
                    : WIN_SCORE / 4;
        }
        int threatGain = Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color));
        if (threatGain >= movedValue * 2) {
            compensation += threatGain / 3;
        }
        int relief = Math.max(0, invadingPieceScore(before, color) - invadingPieceScore(after, color));
        if (relief >= 1_200) {
            compensation += relief;
        }

        Piece moved = after.get(move.destination());
        if (moved != null && knownType(moved) == PieceType.PAWN
                && pawnTrapScore(after, move.destination(), color) >= PAWN_TRAP_BONUS) {
            compensation += movedValue;
        }
        compensation += forcedRecaptureValue(after, move.destination(), color);
        return Math.min(movedValue * 2, compensation);
    }

    private int captureValue(Board board, Piece piece) {
        return captureValue(board, piece, null);
    }

    private int captureUrgencyBonus(Board board, Piece captured, Position target, PlayerColor color) {
        int score = switch (knownType(captured)) {
            case ROOK -> 1_000;
            case CANNON -> 2_200;
            case KNIGHT -> 500;
            case PAWN -> 0;
            case GUARD, BISHOP -> 120;
            case KING -> WIN_SCORE / 4;
        };
        if (isHomeSide(target, color)) {
            score += 350;
        }
        Position king = board.findKing(color);
        if (king != null) {
            int distance = Math.abs(target.x() - king.x()) + Math.abs(target.y() - king.y());
            score += Math.max(0, 6 - distance) * switch (knownType(captured)) {
                case ROOK -> 260;
                case CANNON -> 360;
                case KNIGHT -> 180;
                case PAWN -> 40;
                case GUARD, BISHOP -> 60;
                case KING -> 0;
            };
        }
        return score;
    }

    private int captureValue(Board board, Piece piece, Position position) {
        if (piece.visible()) {
            return value(piece.type());
        }
        return position == null
                ? expectedHiddenValue(board, piece.color())
                : expectedHiddenValue(board, piece.color(), position);
    }

    private int evaluate(Board board, PlayerColor aiColor) {
        if (kingMissing(board, aiColor)) {
            return -WIN_SCORE;
        }
        if (kingMissing(board, aiColor.opponent())) {
            return WIN_SCORE;
        }

        String cacheKey = aiColor.name() + "|" + knownPositionKey(board);
        Integer cached = evalCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        int score = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            int side = piece.color() == aiColor ? 1 : -1;
            int pieceScore = pieceSearchValue(board, piece, position) + activityBonus(piece, position);
            if (!piece.visible()) {
                pieceScore -= 25;
            }
            score += side * pieceScore;
            score += side * attackScore(board, position, piece.color());
        }
        if (ruleEngine.isInCheck(board, aiColor.opponent())) {
            score += CHECK_BONUS;
        }
        if (ruleEngine.isInCheck(board, aiColor)) {
            score -= SELF_CHECK_PENALTY;
        }
        score += kingPressure(board, aiColor) - kingPressure(board, aiColor.opponent());
        score += hangingPieceScore(board, aiColor);
        score -= exposedImportantPieces(board, aiColor);
        score += exposedImportantPieces(board, aiColor.opponent());
        score -= invadingPieceScore(board, aiColor);
        score += invadingPieceScore(board, aiColor.opponent());
        score += immediateThreatScore(board, aiColor) - immediateThreatScore(board, aiColor.opponent());
        score += revealOpportunityScore(board, aiColor) - revealOpportunityScore(board, aiColor.opponent());
        score += jieqiShapePressure(board, aiColor) - jieqiShapePressure(board, aiColor.opponent());
        score += coordinatedKingAttackScore(board, aiColor) - coordinatedKingAttackScore(board, aiColor.opponent());
        score += formationScore(board, aiColor) - formationScore(board, aiColor.opponent());
        score += recordStrategyScore(board, aiColor) - recordStrategyScore(board, aiColor.opponent());
        score += layoutPatternScore(board, aiColor) - layoutPatternScore(board, aiColor.opponent());
        score += sameTypeCoordinationScore(board, aiColor) - sameTypeCoordinationScore(board, aiColor.opponent());
        score += candidatePlanScore(board, aiColor) - candidatePlanScore(board, aiColor.opponent());
        score += pinPressureScore(board, aiColor) - pinPressureScore(board, aiColor.opponent());
        score += recordStructureScore(board, aiColor) - recordStructureScore(board, aiColor.opponent());
        score += pieceRoleScore(board, aiColor) - pieceRoleScore(board, aiColor.opponent());
        score -= opponentPlanThreatScore(board, aiColor) / 2;
        score += opponentPlanThreatScore(board, aiColor.opponent()) / 2;
        score += hiddenInformationScore(board, aiColor) - hiddenInformationScore(board, aiColor.opponent());
        // Hidden piece count advantage: more hidden pieces = more upside potential
        int ownHidden = hiddenPieceCount(board, aiColor);
        int oppHidden = hiddenPieceCount(board, aiColor.opponent());
        score += (ownHidden - oppHidden) * hiddenPhase(board) * 4;
        score += cannonScreenScore(board, aiColor) - cannonScreenScore(board, aiColor.opponent());
        score += knightKillShapeScore(board, aiColor) - knightKillShapeScore(board, aiColor.opponent());
        score += pawnConstrictionScore(board, aiColor) - pawnConstrictionScore(board, aiColor.opponent());
        score += endgameMateNetScore(board, aiColor) - endgameMateNetScore(board, aiColor.opponent());
        score += endgameTechniqueScore(board, aiColor) - endgameTechniqueScore(board, aiColor.opponent());
        score += flyingKingCoordinationScore(board, aiColor) - flyingKingCoordinationScore(board, aiColor.opponent());
        int xiangqiWeight = xiangqiKnowledgeWeight(board);
        score += xiangqiKnowledge.score(board, aiColor, ruleEngine, moveGenerator) * xiangqiWeight / 100;
        if (evalCache.size() < EVAL_CACHE_LIMIT) {
            evalCache.put(cacheKey, score);
        }
        return score;
    }

    private int immediateThreatScore(Board board, PlayerColor color) {
        int best = 0;
        int examined = 0;
        for (Move move : moveGenerator.generateActions(board, color, 0)) {
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Piece mover = board.get(move.source());
            Piece captured = board.get(move.destination());
            // Only evaluate captures and check-giving moves for speed
            boolean isCapture = captured != null;
            Board next = applyForSearch(board, move);
            if (!isCapture && !ruleEngine.isInCheck(next, color.opponent())) {
                continue;
            }
            examined++;
            int score = 0;
            if (captured != null) {
                score += captureValue(board, captured, move.destination()) * 3;
                if (knownType(captured) == PieceType.KING) {
                    score += WIN_SCORE / 3;
                }
                if (mover != null) {
                    score -= pieceSearchValue(board, mover, move.source()) / 4;
                }
            }
            if (ruleEngine.isInCheck(next, color.opponent())) {
                if (!moveGenerator.hasCheckEscape(next, color.opponent())) {
                    score += WIN_SCORE / 4;
                } else {
                    score += CHECK_BONUS / 3;
                }
            } else if (!moveGenerator.hasAnyAction(next, color.opponent())) {
                score += WIN_SCORE / 4;
            }
            Piece moved = next.get(move.destination());
            if (moved != null) {
                int attackers = attackersValue(next, move.destination(), color.opponent());
                int defenders = defendersValue(next, move.destination(), color);
                if (attackers > 0 && defenders == 0) {
                    score -= pieceSearchValue(next, moved, move.destination());
                }
            }
            best = Math.max(best, score);
            if (best >= WIN_SCORE / 4 || examined >= 24) {
                return best;
            }
        }
        return Math.min(4_000, best);
    }

    private int jieqiShapePressure(Board board, PlayerColor color) {
        int hiddenPhase = hiddenPhase(board);
        int score = 0;
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type == PieceType.CANNON) {
                score += cannonLinePressure(board, source, enemyKing, hiddenPhase);
            } else if (type == PieceType.ROOK) {
                score += rookRibPressure(board, source, enemyKing, hiddenPhase);
            }
        }
        return Math.min(1_600, score);
    }

    private int coordinatedAttackProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        PieceType type = knownType(mover);
        int progress = coordinatedKingAttackScore(after, color) - coordinatedKingAttackScore(before, color);
        return Math.max(0, Math.min(COORDINATED_ATTACK_BONUS, progress));
    }

    private int formationProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        int progress = formationScore(after, color) - formationScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(FORMATION_PROGRESS_BONUS, progress);
    }

    private int formationScore(Board board, PlayerColor color) {
        Position enemyKing = board.findKing(color.opponent());
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                continue;
            }
            PieceType type = knownType(piece);
            int rank = color == PlayerColor.RED ? source.y() : Position.HEIGHT - 1 - source.y();
            int centerFile = 4 - Math.abs(source.x() - 4);
            int defenders = defendersValue(board, source, color);
            int attackers = attackersValue(board, source, color.opponent());
            if (defenders > 0) {
                score += Math.min(420, defenders / 2);
            }
            if (attackers > 0 && defenders == 0) {
                score -= pieceSearchValue(board, piece, source) >= HIGH_VALUE_PIECE ? 1_900 : 520;
            }
            switch (type) {
                case ROOK -> {
                    score += centerFile * 55 + Math.min(rank, 7) * 45;
                    if (enemyKing != null && (source.x() == enemyKing.x() || Math.abs(source.x() - enemyKing.x()) == 1)) {
                        score += 420;
                    }
                }
                case CANNON -> {
                    score += centerFile * 45 + (rank >= 4 ? 260 : 80);
                    if (enemyKing != null && source.x() == enemyKing.x()) {
                        int between = board.countBetween(source, enemyKing);
                        score += between == 1 ? 620 : between == 2 ? 260 : 0;
                    }
                }
                case KNIGHT -> {
                    score += centerFile * 50 + (rank >= 3 && rank <= 7 ? 380 : 90);
                    if (enemyKing != null && manhattan(source, enemyKing) <= 4) {
                        score += 360;
                    }
                }
                case PAWN -> {
                    score += rank * 55;
                    if (isOpponentSide(source, color)) {
                        score += 360;
                    }
                    if (enemyKing != null && Math.abs(source.x() - enemyKing.x()) <= 1) {
                        score += 420;
                    }
                }
                case GUARD, BISHOP -> score += ownSide(source, color) ? 120 : 220;
                case KING -> {
                }
            }
            score += nearbyFriendlySupport(board, source, color) * 120;
        }
        score += coordinatedKingAttackScore(board, color) / 2;
        return Math.max(-5_000, Math.min(8_000, score));
    }

    private int layoutPatternProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        int progress = layoutPatternScore(after, color) - layoutPatternScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        if (isOpponentSide(move.destination(), color)) {
            progress += 180;
        }
        Piece moved = after.get(move.destination());
        if (moved != null && knownType(moved) != PieceType.KING
                && attackersValue(after, move.destination(), color.opponent()) > 0
                && defendersValue(after, move.destination(), color) == 0) {
            progress -= pieceSearchValue(after, moved, move.destination()) / 2;
        }
        return Math.max(0, Math.min(LAYOUT_PATTERN_PROGRESS_BONUS, progress));
    }

    private int pieceRoleProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null || knownType(mover) == PieceType.KING) {
            return 0;
        }
        int progress = pieceRoleScore(after, color) - pieceRoleScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(PIECE_ROLE_PROGRESS_BONUS, progress);
    }

    private int pieceRoleScore(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("role", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Position ownKing = board.findKing(color);
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            int rank = forwardRank(source, color);
            int distanceToEnemyKing = manhattan(source, enemyKing);
            int defenders = defendersValue(board, source, color);
            int attackers = attackersValue(board, source, color.opponent());
            switch (type) {
                case ROOK -> {
                    score += openLineMobility(board, source) * 55 + Math.min(7, rank) * 70;
                    if (sameFileOrRank(source, enemyKing)) {
                        int between = board.countBetween(source, enemyKing);
                        score += between <= 1 ? 900 : 260;
                    }
                    if (boardControlsPalace(board, source, color.opponent())) {
                        score += 620;
                    }
                }
                case CANNON -> {
                    score += openLineMobility(board, source) * 35 + (rank >= 4 ? 360 : 120);
                    if (sameFileOrRank(source, enemyKing)) {
                        int between = board.countBetween(source, enemyKing);
                        score += between == 1 ? 1_050 : between == 2 ? 360 : 0;
                    }
                    score += cannonScreenScore(board, color) / 4;
                }
                case KNIGHT -> {
                    score += knightMobility(board, source, color) * 70;
                    if (distanceToEnemyKing <= 4) {
                        score += Math.max(0, 5 - distanceToEnemyKing) * 230;
                    }
                    if (rank >= 3 && rank <= 7) {
                        score += 380;
                    }
                }
                case PAWN -> {
                    score += rank * 85;
                    if (isOpponentSide(source, color)) {
                        score += 420;
                    }
                    if (distanceToEnemyKing <= 3 || ownPalace(source, color.opponent())) {
                        score += 620;
                    }
                    if (defenders > 0 && attackers > 0) {
                        score += 420;
                    }
                }
                case GUARD, BISHOP -> {
                    if (ownKing != null && manhattan(source, ownKing) <= 3) {
                        score += 420;
                    }
                    if (boardControlsPalace(board, source, color.opponent())) {
                        score += 180;
                    }
                    if (attackers > 0 && defenders == 0 && isOpponentSide(source, color)) {
                        score -= 420;
                    }
                }
                case KING -> {
                    score += flyingKingCoordinationScore(board, color) / 2;
                    score += kingBackedMajorLayoutScore(board, source, color, enemyKing) / 2;
                }
            }
            if (attackers > 0 && defenders == 0 && pieceSearchValue(board, piece, source) >= HIGH_VALUE_PIECE) {
                score -= 1_250;
            }
        }
        int result = Math.max(-4_000, Math.min(7_000, score));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int layoutPatternScore(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("layout", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        Position ownKing = board.findKing(color);

        int score = 0;
        int palaceControllers = 0;
        int forwardMajors = 0;
        int supportedInvaders = 0;
        List<Position> rookCannons = new java.util.ArrayList<>();
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type == PieceType.ROOK || type == PieceType.CANNON) {
                rookCannons.add(source);
                if (isOpponentSide(source, color)) {
                    forwardMajors++;
                }
            }
            if (boardControlsPalace(board, source, color.opponent())) {
                palaceControllers++;
            }
            if (isOpponentSide(source, color) && nearbyFriendlySupport(board, source, color) > 0) {
                supportedInvaders++;
            }
            score += baitTrapLayoutScore(board, source, color, enemyKing);
        }

        for (int i = 0; i < rookCannons.size(); i++) {
            for (int j = i + 1; j < rookCannons.size(); j++) {
                score += rookCannonLayoutPairScore(board, rookCannons.get(i), rookCannons.get(j), color, enemyKing);
            }
        }

        if (palaceControllers >= 3) {
            score += 1_250;
        } else if (palaceControllers == 2) {
            score += 720;
        }
        if (forwardMajors >= 2) {
            score += 760;
        }
        if (supportedInvaders >= 3) {
            score += 640;
        } else if (supportedInvaders == 2) {
            score += 360;
        }
        score += kingBackedMajorLayoutScore(board, ownKing, color, enemyKing);
        score += coordinatedKingAttackScore(board, color) / 3;

        int result = Math.max(-2_500, Math.min(7_000, score));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int pinPressureScore(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("pin", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KING) {
                continue;
            }
            if (!sameFileOrRank(source, enemyKing)) {
                continue;
            }
            int between = board.countBetween(source, enemyKing);
            if (type == PieceType.ROOK && between == 1) {
                Piece pinned = singlePieceBetween(board, source, enemyKing);
                if (pinned == null || pinned.color() != color.opponent() || knownType(pinned) == PieceType.KING) {
                    continue;
                }
                score += 820 + replayLikeThreatValue(knownType(pinned)) / 2;
            } else if (type == PieceType.CANNON && between == 2) {
                Position pinnedPosition = pinnedVictimPosition(board, source, enemyKing, type);
                Piece pinned = pinnedPosition == null ? null : board.get(pinnedPosition);
                if (pinned == null || pinned.color() != color.opponent() || knownType(pinned) == PieceType.KING) {
                    continue;
                }
                score += 620 + replayLikeThreatValue(knownType(pinned)) / 3;
            } else if (type == PieceType.KING && between == 1) {
                Piece pinned = singlePieceBetween(board, source, enemyKing);
                if (pinned == null || pinned.color() != color.opponent() || knownType(pinned) == PieceType.KING) {
                    continue;
                }
                score += 520;
            }
        }
        int result = Math.min(PIN_PRESSURE_BONUS * 3, score);
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int forcingTacticProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        PieceType type = knownType(moved);
        int score = Math.max(0, pinPressureScore(after, color) - pinPressureScore(before, color));
        score += linePinTacticScore(after, move.destination(), type, color);
        if (ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent())) {
            score += checkingForkTacticScore(after, move.destination(), type, color);
        }
        if (score == 0) {
            return 0;
        }
        int safetyCost = 0;
        int movedValue = pieceSearchValue(after, moved, move.destination());
        if (legalAttackersValue(after, move.destination(), color.opponent()) > defendersValue(after, move.destination(), color)) {
            safetyCost = movedValue / 2;
            if (movedValue >= value(PieceType.CANNON)) {
                safetyCost += movedValue / 2;
            }
        }
        int progress = Math.max(0, score - safetyCost);
        return Math.min(FORCING_TACTIC_PROGRESS_BONUS * 2, progress);
    }

    private int linePinTacticScore(Board board, Position source, PieceType type, PlayerColor color) {
        if (type != PieceType.ROOK && type != PieceType.CANNON) {
            return 0;
        }
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null || !sameFileOrRank(source, enemyKing)) {
            return 0;
        }
        int between = board.countBetween(source, enemyKing);
        int requiredBetween = type == PieceType.ROOK ? 1 : 2;
        if (between != requiredBetween) {
            return 0;
        }
        Position victimPosition = pinnedVictimPosition(board, source, enemyKing, type);
        if (victimPosition == null) {
            return 0;
        }
        Piece victim = board.get(victimPosition);
        if (victim == null || victim.color() != color.opponent() || knownType(victim) == PieceType.KING) {
            return 0;
        }
        int score = 900 + replayLikeThreatValue(knownType(victim)) / (type == PieceType.ROOK ? 1 : 2);
        if (isOpponentSide(source, color)) {
            score += 420;
        }
        return score;
    }

    private Position pinnedVictimPosition(Board board, Position source, Position enemyKing, PieceType type) {
        if (type == PieceType.ROOK) {
            return singlePieceBetweenPosition(board, source, enemyKing);
        }
        int dx = Integer.compare(enemyKing.x(), source.x());
        int dy = Integer.compare(enemyKing.y(), source.y());
        Position lastPiece = null;
        int x = source.x() + dx;
        int y = source.y() + dy;
        while (x != enemyKing.x() || y != enemyKing.y()) {
            Position current = new Position(x, y);
            Piece piece = board.get(current);
            if (piece != null) {
                lastPiece = current;
            }
            x += dx;
            y += dy;
        }
        return lastPiece;
    }

    private int checkingForkTacticScore(Board board, Position source, PieceType type, PlayerColor color) {
        if (type == PieceType.PAWN || type == PieceType.GUARD || type == PieceType.BISHOP) {
            return 0;
        }
        int best = 0;
        for (Position target : board.occupiedPositions()) {
            Piece victim = board.get(target);
            if (victim == null || victim.color() != color.opponent() || knownType(victim) == PieceType.KING) {
                continue;
            }
            if (!ruleEngine.canMove(board, source, target, color)) {
                continue;
            }
            int value = replayLikeThreatValue(knownType(victim));
            if (victim.visible()) {
                value += pieceSearchValue(board, victim, target) / 2;
            } else {
                value += expectedHiddenValue(board, victim.color(), target) / 3;
            }
            best = Math.max(best, value);
        }
        if (best < value(PieceType.PAWN)) {
            return 0;
        }
        return 700 + best;
    }

    private int rookCannonLayoutPairScore(
            Board board,
            Position first,
            Position second,
            PlayerColor color,
            Position enemyKing) {
        Piece firstPiece = board.get(first);
        Piece secondPiece = board.get(second);
        if (firstPiece == null || secondPiece == null) {
            return 0;
        }
        PieceType firstType = knownType(firstPiece);
        PieceType secondType = knownType(secondPiece);
        if (!((firstType == PieceType.ROOK && secondType == PieceType.CANNON)
                || (firstType == PieceType.CANNON && secondType == PieceType.ROOK)
                || (firstType == PieceType.ROOK && secondType == PieceType.ROOK)
                || (firstType == PieceType.CANNON && secondType == PieceType.CANNON))) {
            return 0;
        }

        int score = 0;
        boolean firstLinesKing = sameFileOrRank(first, enemyKing);
        boolean secondLinesKing = sameFileOrRank(second, enemyKing);
        if (firstLinesKing && secondLinesKing) {
            score += 620;
            if (first.x() == second.x() || first.y() == second.y()) {
                score += 520;
            }
        } else if (firstLinesKing || secondLinesKing) {
            score += 260;
        }

        if (firstType != secondType && (firstLinesKing || secondLinesKing)) {
            Position cannon = firstType == PieceType.CANNON ? first : second;
            Position rook = firstType == PieceType.ROOK ? first : second;
            if (sameFileOrRank(cannon, enemyKing)) {
                int screens = board.countBetween(cannon, enemyKing);
                score += screens == 1 ? 1_050 : screens == 2 ? 360 : 0;
            }
            if (sameFileOrRank(rook, enemyKing)) {
                int blockers = board.countBetween(rook, enemyKing);
                score += blockers <= 1 ? 520 : 160;
            }
        }

        int pairDistance = manhattan(first, second);
        if (pairDistance <= 4) {
            score += Math.max(0, 5 - pairDistance) * 120;
        }
        if (isOpponentSide(first, color) && isOpponentSide(second, color)) {
            score += 460;
        }
        return score;
    }

    private int baitTrapLayoutScore(Board board, Position source, PlayerColor color, Position enemyKing) {
        Piece piece = board.get(source);
        if (piece == null || piece.color() != color) {
            return 0;
        }
        PieceType type = knownType(piece);
        if (type != PieceType.PAWN && type != PieceType.GUARD && type != PieceType.BISHOP) {
            return 0;
        }
        if (!isOpponentSide(source, color) && manhattan(source, enemyKing) > 4) {
            return 0;
        }

        int score = 0;
        int majorDefenders = majorDefenderCount(board, source, color);
        int attackers = attackersValue(board, source, color.opponent());
        if (majorDefenders > 0) {
            score += type == PieceType.PAWN ? 580 : 260;
            score += majorDefenders * 260;
        }
        if (attackers > 0 && majorDefenders > 0) {
            score += type == PieceType.PAWN ? 900 : 420;
        }
        if (boardControlsPalace(board, source, color.opponent())) {
            score += 520;
        }
        if (type == PieceType.PAWN && manhattan(source, enemyKing) <= 3) {
            score += 460;
        }
        if (attackers > 0 && majorDefenders == 0) {
            score -= 420;
        }
        return score;
    }

    private int kingBackedMajorLayoutScore(Board board, Position ownKing, PlayerColor color, Position enemyKing) {
        if (ownKing == null) {
            return 0;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type != PieceType.ROOK && type != PieceType.CANNON) {
                continue;
            }
            if (!sameFileOrRank(ownKing, source)) {
                continue;
            }
            int blockers = board.countBetween(ownKing, source);
            if (blockers > 0) {
                continue;
            }

            int rank = forwardRank(source, color);
            int majorScore = type == PieceType.ROOK ? 520 : 420;
            majorScore += Math.min(7, rank) * 70;
            if (sameFileOrRank(source, enemyKing)) {
                int betweenEnemy = board.countBetween(source, enemyKing);
                majorScore += type == PieceType.ROOK
                        ? (betweenEnemy <= 1 ? 720 : 240)
                        : (betweenEnemy == 1 ? 820 : betweenEnemy == 2 ? 280 : 0);
            }
            if (boardControlsPalace(board, source, color.opponent())) {
                majorScore += 460;
            }
            if (isOpponentSide(source, color)) {
                majorScore += 320;
            }
            score += majorScore;
        }
        return Math.min(2_200, score);
    }

    private int majorDefenderCount(Board board, Position target, PlayerColor color) {
        Board targetRemoved = board.copy();
        targetRemoved.remove(target);
        int count = 0;
        for (Position source : board.occupiedPositions()) {
            if (source.equals(target)) {
                continue;
            }
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type != PieceType.ROOK && type != PieceType.CANNON && type != PieceType.KNIGHT) {
                continue;
            }
            if (ruleEngine.canMoveAndKeepKingSafe(targetRemoved, source, target, color)) {
                count++;
            }
        }
        return Math.min(3, count);
    }

    private int recordStrategyProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        int progress = recordStrategyScore(after, color) - recordStrategyScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(RECORD_STRATEGY_PROGRESS_BONUS, progress);
    }

    private int recordStructureScore(Board board, PlayerColor color) {
        Map<String, Integer> weights = recordStructureWeights();
        if (weights.isEmpty()) {
            return 0;
        }
        int score = 0;
        int phase = Math.max(20, xiangqiKnowledgeWeight(board));
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                continue;
            }
            String key = knownType(piece).name() + "@" + zoneKey(source, color);
            score += weights.getOrDefault(key, 0);
            if (boardControlsPalace(board, source, color.opponent())) {
                score += weights.getOrDefault(knownType(piece).name() + "@PALACE_PRESSURE", 0);
            }
        }
        return Math.max(-RECORD_STRUCTURE_BONUS, Math.min(RECORD_STRUCTURE_BONUS, score * phase / 100));
    }

    private Map<String, Integer> recordStructureWeights() {
        if (recordStructureWeights != null) {
            return recordStructureWeights;
        }
        Map<String, Integer> weights = new HashMap<>();
        Path records = Path.of("records");
        if (!Files.isDirectory(records)) {
            recordStructureWeights = weights;
            return weights;
        }
        try (var stream = Files.list(records)) {
            stream.filter(path -> path.getFileName().toString().startsWith("game-")
                            && path.getFileName().toString().endsWith(".jsonl"))
                    .limit(80)
                    .forEach(path -> loadRecordStructureWeights(path, weights));
        } catch (IOException ignored) {
            // Optional strategy statistics; ignore unreadable records.
        }
        weights.replaceAll((key, value) -> Math.max(-420, Math.min(420, value)));
        recordStructureWeights = weights;
        return weights;
    }

    private void loadRecordStructureWeights(Path path, Map<String, Integer> weights) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.size() < 8) {
                return;
            }
            String last = lines.get(lines.size() - 1);
            PlayerColor winner = last.contains("\"winner\":\"red\"")
                    ? PlayerColor.RED
                    : last.contains("\"winner\":\"black\"") ? PlayerColor.BLACK : null;
            if (winner == null) {
                return;
            }
            Board board = parseRecordInitialBoard(lines.get(0));
            for (String line : lines.subList(1, lines.size())) {
                String colorText = jsonStringValue(line, "color");
                String sourceText = jsonStringValue(line, "source");
                String destinationText = jsonStringValue(line, "destination");
                if (colorText == null || sourceText == null || destinationText == null) {
                    continue;
                }
                PlayerColor moverColor = "red".equals(colorText) ? PlayerColor.RED : PlayerColor.BLACK;
                Position source = Position.parse(sourceText);
                Position destination = Position.parse(destinationText);
                Piece mover = board.get(source);
                Piece captured = board.get(destination);
                if (mover != null) {
                    int delta = moverColor == winner ? 18 : -12;
                    String key = knownType(mover).name() + "@" + zoneKey(destination, moverColor);
                    weights.merge(key, delta, Integer::sum);
                    if (captured != null && captured.color() != moverColor) {
                        weights.merge(knownType(mover).name() + "@CAPTURE", delta / 2, Integer::sum);
                    }
                    board.remove(source);
                    board.set(destination, mover);
                    if (!mover.visible()) {
                        mover.reveal();
                    }
                }
            }
        } catch (Exception ignored) {
            // A malformed old record should not affect AI play.
        }
    }

    private Board parseRecordInitialBoard(String line) {
        Board board = new Board();
        int arrayStart = line.indexOf("\"board\":[");
        if (arrayStart < 0) {
            return board;
        }
        String body = line.substring(arrayStart + 9, line.lastIndexOf(']'));
        for (String item : body.split("\\},\\{")) {
            String object = item.replace("{", "").replace("}", "");
            String positionText = jsonStringValue("{" + object + "}", "position");
            String colorText = jsonStringValue("{" + object + "}", "color");
            String typeText = jsonStringValue("{" + object + "}", "type");
            String hiddenText = jsonStringValue("{" + object + "}", "hiddenMoveType");
            boolean visible = object.contains("\"visible\":true");
            if (positionText == null || colorText == null || typeText == null || hiddenText == null) {
                continue;
            }
            PlayerColor color = "red".equals(colorText) ? PlayerColor.RED : PlayerColor.BLACK;
            board.set(Position.parse(positionText), new Piece(color,
                    PieceType.valueOf(typeText.toUpperCase()),
                    PieceType.valueOf(hiddenText.toUpperCase()),
                    visible));
        }
        return board;
    }

    private String jsonStringValue(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private String zoneKey(Position position, PlayerColor color) {
        if (ownPalace(position, color.opponent())) {
            return "ENEMY_PALACE";
        }
        if (isOpponentSide(position, color)) {
            return "ENEMY_HALF";
        }
        int file = position.x() <= 2 ? 0 : position.x() >= 6 ? 2 : 1;
        return "HOME_" + file;
    }

    private int strategicPlanProgressScore(Board before, Board after, Move move, PlayerColor color) {
        if (xiangqiKnowledgeWeight(before) < 25) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        int beforePlan = strategicPlanPotential(before, color);
        int afterPlan = strategicPlanPotential(after, color);
        int progress = afterPlan - beforePlan;
        if (progress <= 0) {
            return 0;
        }
        return Math.min(STRATEGIC_PLAN_PROGRESS_BONUS, progress);
    }

    private int candidatePlanProgressScore(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        int progress = candidatePlanScore(after, color) - candidatePlanScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        return Math.min(CANDIDATE_PLAN_BONUS, progress);
    }

    private int candidatePlanScore(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("candidatePlan", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int attack = coordinatedKingAttackScore(board, color)
                + layoutPatternScore(board, color) / 2
                + pinPressureScore(board, color)
                + endgameMateNetScore(board, color) / 2;
        int defense = Math.max(0, opponentPlanThreatScore(board, color) - kingDangerScoreLight(board, color))
                + invadingPieceScore(board, color) / 2
                + specificThreatShapeScore(board, color) / 2;
        int reveal = hiddenInformationScore(board, color)
                + remainingHighValueHiddenCount(board, color) * Math.max(0, hiddenPhase(board)) * 4
                - importantPieceExposure(board, color) / 2;
        int exchange = strategicExchangePotential(board, color);
        int best = Math.max(Math.max(attack, defense), Math.max(reveal, exchange));
        int second = secondBest(attack, defense, reveal, exchange);
        int score = best + second / 3;
        if (attack >= 2_800 && defense >= 2_400) {
            score += 520;
        }
        int result = Math.max(-2_500, Math.min(7_500, score));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int secondBest(int first, int second, int third, int fourth) {
        int best = Integer.MIN_VALUE;
        int runnerUp = Integer.MIN_VALUE;
        for (int value : new int[]{first, second, third, fourth}) {
            if (value > best) {
                runnerUp = best;
                best = value;
            } else if (value > runnerUp) {
                runnerUp = value;
            }
        }
        return runnerUp == Integer.MIN_VALUE ? 0 : runnerUp;
    }

    private int kingDangerScoreLight(Board board, PlayerColor color) {
        int score = ruleEngine.isInCheck(board, color) ? 2_000 : 0;
        score += Math.max(0, 3 - legalKingMoveCount(board, color)) * 500;
        return score;
    }

    private int strategicExchangePotential(Board board, PlayerColor color) {
        int best = 0;
        int searched = 0;
        for (Move move : moveGenerator.generateActions(board, color, 0)) {
            if (searched++ >= 18) {
                break;
            }
            Piece captured = board.get(move.destination());
            if (captured == null || captured.color() != color.opponent()
                    || !ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            best = Math.max(best, strategicExchangeScore(board, next, move, color));
        }
        return Math.max(0, best);
    }

    private int opponentIntentDisruptionScore(Board before, Board after, Move move, PlayerColor color) {
        int beforeIntent = opponentBestIntentScore(before, color);
        int afterIntent = opponentBestIntentScore(after, color);
        int drop = beforeIntent - afterIntent;
        if (drop <= 0) {
            return 0;
        }
        int score = drop * 2;
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            score += captureUrgencyBonus(before, captured, move.destination(), color);
        }
        return Math.min(OPPONENT_INTENT_BONUS, score);
    }

    private int opponentBestIntentScore(Board board, PlayerColor defender) {
        String cacheKey = heuristicKey("intent", board, defender);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        PlayerColor attacker = defender.opponent();
        int best = opponentPlanThreatScore(board, defender);
        int searched = 0;
        for (Move move : lightweightIntentMoves(board, attacker)) {
            if (searched++ >= 14) {
                break;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), attacker)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            int score = 0;
            if (ruleEngine.isInCheck(next, defender)) {
                score += moveGenerator.hasCheckEscape(next, defender) ? 1_800 : WIN_SCORE / 3;
            }
            Piece captured = board.get(move.destination());
            if (captured != null && captured.color() == defender) {
                score += captureValue(board, captured, move.destination()) * 2;
            }
            score += Math.max(0, opponentPlanThreatScore(next, defender) - opponentPlanThreatScore(board, defender));
            score += Math.max(0, layoutPatternScore(next, attacker) - layoutPatternScore(board, attacker));
            score += Math.max(0, pinPressureScore(next, attacker) - pinPressureScore(board, attacker));
            best = Math.max(best, score);
        }
        int result = Math.min(9_000, Math.max(0, best));
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private List<Move> lightweightIntentMoves(Board board, PlayerColor color) {
        return moveGenerator.generateActions(board, color, 0).stream()
                .filter(move -> ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color))
                .sorted(Comparator.comparingInt((Move move) -> lightweightIntentMoveScore(board, move, color)).reversed())
                .limit(14)
                .toList();
    }

    private int lightweightIntentMoveScore(Board board, Move move, PlayerColor color) {
        String cacheKey = "lightIntentMove|" + color.name() + "|" + move.notation() + "|" + knownPositionKey(board);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int score = 0;
        Piece captured = board.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            score += 2_000 + captureValue(board, captured, move.destination()) * 3;
        }
        Board next = applyForSearch(board, move);
        if (ruleEngine.isInCheck(next, color.opponent())) {
            score += 2_800;
        }
        Piece mover = board.get(move.source());
        if (mover != null) {
            PieceType type = knownType(mover);
            if (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT) {
                score += 420;
            } else if (type == PieceType.PAWN && isOpponentSide(move.destination(), color)) {
                score += 360;
            }
        }
        score += Math.max(0, layoutPatternScore(next, color) - layoutPatternScore(board, color)) / 2;
        score += Math.max(0, pinPressureScore(next, color) - pinPressureScore(board, color)) / 2;
        heuristicScores.put(cacheKey, score);
        return score;
    }

    private int adaptiveTempoScore(Board before, Board after, Move move, PlayerColor color) {
        int myAttack = candidatePlanScore(before, color);
        int enemyAttack = opponentBestIntentScore(before, color);
        int material = materialBalance(before, color);
        int score = 0;
        if (myAttack >= enemyAttack + 1_200) {
            score += Math.max(0, candidatePlanScore(after, color) - myAttack);
            score -= Math.max(0, opponentBestIntentScore(after, color) - enemyAttack) / 2;
            if (ruleEngine.isInCheck(after, color.opponent())) {
                score += 420;
            }
        } else if (enemyAttack >= myAttack + 900) {
            score += Math.max(0, enemyAttack - opponentBestIntentScore(after, color)) * 2;
            score += urgentDefenseScore(before, after, move, color);
        } else {
            score += Math.max(0, layoutPatternScore(after, color) - layoutPatternScore(before, color));
            score += Math.max(0, pieceRoleScore(after, color) - pieceRoleScore(before, color)) / 2;
        }
        if (material > 900) {
            score += strategicExchangeScore(before, after, move, color) / 2;
            score -= hiddenWorstCaseRiskPenalty(before, after, move, color) / 2;
        } else if (material < -700) {
            score += Math.max(0, candidatePlanScore(after, color) - candidatePlanScore(before, color));
            Piece captured = before.get(move.destination());
            if (captured == null && !ruleEngine.isInCheck(after, color.opponent())) {
                score -= 180;
            }
        }
        return Math.max(-900, Math.min(ADAPTIVE_TEMPO_BONUS, score));
    }

    private int strategicPlanPotential(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("plan", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int baseRecord = recordStrategyScore(board, color);
        int baseAttack = coordinatedKingAttackScore(board, color);
        int basePressure = kingPressure(board, color);
        int baseLayout = layoutPatternScore(board, color);
        int best = 0;
        int second = 0;
        int searched = 0;
        for (Move followup : moveGenerator.generateActions(board, color, 0)) {
            if (searched >= 12) {
                break;
            }
            Piece mover = board.get(followup.source());
            if (mover == null || knownType(mover) == PieceType.KING) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, followup.source(), followup.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, followup);
            int exposure = exposedMovePenalty(board, next, followup, color)
                    + obviousGiftPenalty(board, next, followup, color) / 3;
            if (exposure >= OBVIOUS_GIFT_PENALTY) {
                continue;
            }
            int score = Math.max(0, recordStrategyScore(next, color) - baseRecord)
                    + Math.max(0, coordinatedKingAttackScore(next, color) - baseAttack)
                    + Math.max(0, kingPressure(next, color) - basePressure) * 3
                    + Math.max(0, layoutPatternScore(next, color) - baseLayout)
                    + followupPlanShapeBonus(board, next, followup, color)
                    - exposure / 2;
            if (board.get(followup.destination()) != null) {
                score += captureValue(board, board.get(followup.destination()), followup.destination()) / 2;
            }
            if (score > best) {
                second = best;
                best = score;
            } else if (score > second) {
                second = score;
            }
            searched++;
        }
        int result = Math.min(3_000, best + second / 2);
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int followupPlanShapeBonus(Board before, Board after, Move move, PlayerColor color) {
        Piece mover = before.get(move.source());
        if (mover == null) {
            return 0;
        }
        Position enemyKing = before.findKing(color.opponent());
        PieceType type = knownType(mover);
        int rank = forwardRank(move.destination(), color);
        int score = 0;
        if (type == PieceType.KNIGHT && rank >= 3 && rank <= 7) {
            score += 360;
        } else if (type == PieceType.PAWN && rank >= 5) {
            score += 420;
        } else if ((type == PieceType.ROOK || type == PieceType.CANNON) && enemyKing != null
                && (move.destination().x() == enemyKing.x() || move.destination().y() == enemyKing.y())) {
            score += type == PieceType.CANNON ? 520 : 460;
        }
        if (enemyKing != null && manhattan(move.destination(), enemyKing) < manhattan(move.source(), enemyKing)) {
            score += 180;
        }
        if (nearbyFriendlySupport(after, move.destination(), color) >= 2) {
            score += 220;
        }
        return score;
    }

    private int hiddenInformationProgressScore(Board before, Board after, Move move, PlayerColor color) {
        int hiddenPhase = hiddenPhase(before);
        if (hiddenPhase < 18) {
            return 0;
        }
        int progress = hiddenInformationScore(after, color) - hiddenInformationScore(before, color);
        if (progress <= 0) {
            return 0;
        }
        Piece mover = before.get(move.source());
        if (mover != null && !mover.visible()) {
            progress += REVEAL_BONUS + hiddenValueSpread(before, color) / 2;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent() && !captured.visible()) {
            progress += HIDDEN_CAPTURE_BONUS + hiddenInvaderInformationDanger(before, move.destination(), color);
        }
        return Math.min(HIDDEN_INFORMATION_PROGRESS_BONUS + hiddenPhase * 8, progress);
    }

    private int hiddenInformationScore(Board board, PlayerColor color) {
        int hiddenPhase = hiddenPhase(board);
        if (hiddenPhase <= 0) {
            return 0;
        }
        int score = 0;
        Position king = board.findKing(color);
        int ownSpread = hiddenValueSpread(board, color);
        int opponentSpread = hiddenValueSpread(board, color.opponent());
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.visible()) {
                continue;
            }
            if (piece.color() == color) {
                int revealSafety = attackersValue(board, position, color.opponent()) == 0
                        ? 420
                        : defendersValue(board, position, color) > 0 ? 180 : -520;
                int rank = forwardRank(position, color);
                score += revealSafety + ownSpread / 4 + rank * 28;
                if (knownType(piece) == PieceType.ROOK || knownType(piece) == PieceType.CANNON) {
                    score -= Math.max(0, value(knownType(piece)) - expectedHiddenValue(board, color, position)) / 2;
                }
            } else {
                int danger = hiddenInvaderInformationDanger(board, position, color);
                if (danger > 0) {
                    score -= danger + opponentSpread / 3;
                }
                if (king != null && manhattan(position, king) <= 3) {
                    score -= expectedHiddenValue(board, piece.color(), position) / 2;
                }
            }
        }
        return Math.max(-4_500, Math.min(4_500, score * hiddenPhase / 100));
    }

    private int hiddenValueSpread(Board board, PlayerColor color) {
        Map<PieceType, Integer> counts = remainingHiddenTypeCounts(board, color);
        int total = 0;
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (Map.Entry<PieceType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() <= 0 || entry.getKey() == PieceType.KING) {
                continue;
            }
            int value = value(entry.getKey());
            total += entry.getValue();
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return total == 0 || min == Integer.MAX_VALUE ? 0 : max - min;
    }

    private int hiddenInvaderInformationDanger(Board board, Position position, PlayerColor defender) {
        Piece piece = board.get(position);
        if (piece == null || piece.color() == defender || piece.visible()) {
            return 0;
        }
        Position king = board.findKing(defender);
        int danger = isHomeSide(position, defender) ? 520 : 0;
        if (king != null) {
            int distance = manhattan(position, king);
            danger += Math.max(0, 7 - distance) * 120;
            if (sameFileOrRank(position, king)) {
                danger += 360;
            }
        }
        danger += expectedHiddenValue(board, piece.color(), position) / 3;
        return danger;
    }

    private int recordStrategyScore(Board board, PlayerColor color) {
        String cacheKey = heuristicKey("record", board, color);
        Integer cached = heuristicScores.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int phaseWeight = xiangqiKnowledgeWeight(board);
        if (phaseWeight <= 0) {
            return 0;
        }
        int score = 0;
        Position ownKing = board.findKing(color);
        Position enemyKing = board.findKing(color.opponent());
        int majorPressure = 0;
        int knightPressure = 0;
        int pawnPressure = 0;
        int activeMajors = 0;
        int exposedMajors = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                continue;
            }
            PieceType type = knownType(piece);
            int visibility = piece.visible() ? 100 : 35;
            int rank = forwardRank(source, color);
            int centerFile = 4 - Math.abs(source.x() - 4);
            int pieceScore = 0;
            if (type == PieceType.KNIGHT) {
                pieceScore += knightRecordScore(board, source, color, rank, centerFile);
                if (enemyKing != null && manhattan(source, enemyKing) <= 5) {
                    knightPressure += 1;
                }
            } else if (type == PieceType.ROOK) {
                int line = rookRecordLineScore(board, source, enemyKing, rank, centerFile);
                pieceScore += line;
                majorPressure += line;
                if (line >= 420 || openLineMobility(board, source) >= 5) {
                    activeMajors++;
                }
            } else if (type == PieceType.CANNON) {
                int line = cannonRecordLineScore(board, source, enemyKing, rank, centerFile);
                pieceScore += line;
                majorPressure += line;
                if (line >= 420 || openLineMobility(board, source) >= 4) {
                    activeMajors++;
                }
            } else if (type == PieceType.PAWN) {
                pieceScore += pawnRecordScore(board, source, color, enemyKing, rank, centerFile);
                if (rank >= 5 && enemyKing != null && manhattan(source, enemyKing) <= 5) {
                    pawnPressure++;
                }
            } else if (type == PieceType.GUARD || type == PieceType.BISHOP) {
                pieceScore += ownSide(source, color) ? 80 : 160;
                if (ownKing != null && manhattan(source, ownKing) <= 3) {
                    pieceScore += 90;
                }
            }
            if ((type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT)
                    && attackersValue(board, source, color.opponent()) > 0
                    && defendersValue(board, source, color) == 0) {
                exposedMajors++;
                pieceScore -= type == PieceType.ROOK ? 1_100 : 760;
            }
            score += pieceScore * visibility / 100;
        }
        if (majorPressure >= 700 && knightPressure > 0) {
            score += 620;
        }
        if (majorPressure >= 700 && pawnPressure > 0) {
            score += 760;
        }
        if (activeMajors >= 2) {
            score += 520;
        }
        if (activeMajors == 0 && strategicPhase(board, color) >= LATE_MAJOR_PHASE) {
            score -= 900;
        }
        score -= exposedMajors * 680;
        score -= passiveKingRecordPenalty(board, color, ownKing);
        int result = Math.max(-7_000, Math.min(9_000, score)) * phaseWeight / 100;
        heuristicScores.put(cacheKey, result);
        return result;
    }

    private int knightRecordScore(Board board, Position source, PlayerColor color, int rank, int centerFile) {
        int score = centerFile * 65;
        if (rank >= 3 && rank <= 7) {
            score += 520;
        } else if (rank <= 1) {
            score -= 220;
        }
        int blockedLegs = blockedKnightLegs(board, source);
        score -= blockedLegs * 180;
        score += knightMobility(board, source, color) * 55;
        return score;
    }

    private int rookRecordLineScore(Board board, Position source, Position enemyKing, int rank, int centerFile) {
        int score = centerFile * 45 + Math.min(rank, 7) * 55 + openLineMobility(board, source) * 35;
        if (enemyKing != null && (source.x() == enemyKing.x() || source.y() == enemyKing.y())) {
            int between = board.countBetween(source, enemyKing);
            score += between == 0 ? 1_150 : between == 1 ? 520 : 160;
        } else if (enemyKing != null && Math.abs(source.x() - enemyKing.x()) == 1) {
            score += 380;
        }
        return score;
    }

    private int cannonRecordLineScore(Board board, Position source, Position enemyKing, int rank, int centerFile) {
        int score = centerFile * 40 + (rank >= 4 ? 360 : 90) + openLineMobility(board, source) * 26;
        if (enemyKing != null && (source.x() == enemyKing.x() || source.y() == enemyKing.y())) {
            int between = board.countBetween(source, enemyKing);
            score += between == 1 ? 1_220 : between == 2 ? 420 : between == 0 ? 120 : 0;
        }
        return score;
    }

    private int pawnRecordScore(
            Board board,
            Position source,
            PlayerColor color,
            Position enemyKing,
            int rank,
            int centerFile) {
        int score = rank * 70;
        if (rank >= 5) {
            score += 620 + centerFile * 70;
        }
        if (enemyKing != null) {
            int distance = manhattan(source, enemyKing);
            score += Math.max(0, 7 - distance) * (rank >= 5 ? 120 : 35);
            if (Math.abs(source.x() - enemyKing.x()) <= 1 && rank >= 5) {
                score += 520;
            }
        }
        score += connectedPawnCount(board, source, color) * 160;
        if (rank >= 5 && defendersValue(board, source, color) > 0) {
            score += 240;
        }
        return score;
    }

    private int passiveKingRecordPenalty(Board board, PlayerColor color, Position king) {
        if (king == null) {
            return WIN_SCORE / 5;
        }
        int penalty = inPalace(king, color) ? 0 : 1_400;
        int homeRank = forwardRank(king, color);
        if (strategicPhase(board, color) < LATE_MAJOR_PHASE) {
            penalty += homeRank * 260;
        } else {
            penalty += Math.max(0, homeRank - 1) * 120;
        }
        if (king.x() != 4) {
            penalty += 180;
        }
        return penalty;
    }

    private int openLineMobility(Board board, Position source) {
        int mobility = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] direction : directions) {
            int x = source.x() + direction[0];
            int y = source.y() + direction[1];
            while (Position.isInside(x, y) && board.isEmpty(new Position(x, y))) {
                mobility++;
                x += direction[0];
                y += direction[1];
            }
        }
        return mobility;
    }

    private int knightMobility(Board board, Position source, PlayerColor color) {
        int mobility = 0;
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                if (ruleEngine.canMove(board, source, new Position(x, y), color)) {
                    mobility++;
                }
            }
        }
        return mobility;
    }

    private int blockedKnightLegs(Board board, Position source) {
        int blocked = 0;
        int[][] legs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] leg : legs) {
            int x = source.x() + leg[0];
            int y = source.y() + leg[1];
            if (Position.isInside(x, y) && !board.isEmpty(new Position(x, y))) {
                blocked++;
            }
        }
        return blocked;
    }

    private int connectedPawnCount(Board board, Position source, PlayerColor color) {
        int connected = 0;
        for (int dx : new int[]{-1, 1}) {
            int x = source.x() + dx;
            if (!Position.isInside(x, source.y())) {
                continue;
            }
            Piece neighbor = board.get(new Position(x, source.y()));
            if (neighbor != null && neighbor.color() == color && knownType(neighbor) == PieceType.PAWN) {
                connected++;
            }
        }
        return connected;
    }

    private int nearbyFriendlySupport(Board board, Position source, PlayerColor color) {
        int support = 0;
        for (Position other : board.occupiedPositions()) {
            if (other.equals(source) || manhattan(source, other) > 3) {
                continue;
            }
            Piece piece = board.get(other);
            if (piece != null && piece.color() == color && knownType(piece) != PieceType.KING) {
                support++;
            }
        }
        return Math.min(3, support);
    }

    private int coordinatedKingAttackScore(Board board, PlayerColor color) {
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return WIN_SCORE / 5;
        }
        int participants = 0;
        int pressure = 0;
        int majorParticipants = 0;
        boolean rookPressure = false;
        boolean cannonPressure = false;
        boolean knightPressure = false;
        boolean pawnPressure = false;
        boolean supportPressure = false;
        boolean kingPressure = false;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            PieceType type = knownType(piece);
            int contribution = piecePressureContribution(board, source, type, enemyKing, color);
            if (contribution <= 0) {
                continue;
            }
            participants++;
            pressure += contribution;
            if (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT) {
                majorParticipants++;
                rookPressure |= type == PieceType.ROOK;
                cannonPressure |= type == PieceType.CANNON;
                knightPressure |= type == PieceType.KNIGHT;
            } else if (type == PieceType.PAWN) {
                pawnPressure = true;
            } else if (type == PieceType.KING) {
                kingPressure = true;
            } else {
                supportPressure = true;
            }
        }
        if (participants < 2) {
            return Math.min(420, pressure / 3);
        }
        int score = pressure + COORDINATED_ATTACK_BONUS;
        if (majorParticipants == 0) {
            score = pressure / 2;
        }
        if (rookPressure && cannonPressure) {
            score += 650;
        }
        if (knightPressure && (rookPressure || cannonPressure)) {
            score += 520;
        }
        if (pawnPressure && majorParticipants > 0) {
            score += 780;
        }
        if (supportPressure && majorParticipants > 0) {
            score += 260;
        }
        if (kingPressure && majorParticipants > 0) {
            score += 360;
        }
        return Math.min(4_600, score);
    }

    private int piecePressureContribution(
            Board board,
            Position source,
            PieceType type,
            Position enemyKing,
            PlayerColor color) {
        int distance = manhattan(source, enemyKing);
        int score = 0;
        if (source.x() == enemyKing.x() || source.y() == enemyKing.y()) {
            int between = board.countBetween(source, enemyKing);
            if (type == PieceType.ROOK) {
                score += between <= 1 ? 760 : 220;
            } else if (type == PieceType.CANNON) {
                score += between == 1 ? 880 : between == 2 ? 280 : 0;
            } else if (type == PieceType.KING && source.x() == enemyKing.x()) {
                score += between == 0 ? WIN_SCORE / 5 : between == 1 ? 520 : 180;
            }
        }
        if (type == PieceType.KNIGHT && distance <= 4 && isOpponentSide(source, color)) {
            score += Math.max(0, 5 - distance) * 180;
        }
        if (type == PieceType.PAWN && isOpponentSide(source, color)) {
            score += Math.max(0, 8 - distance) * 170;
            if (Math.abs(source.x() - enemyKing.x()) <= 1
                    && Math.abs(source.y() - enemyKing.y()) <= 4) {
                score += 620;
            }
            if (ownPalace(source, color.opponent())) {
                score += 820;
            }
        }
        if ((type == PieceType.GUARD || type == PieceType.BISHOP) && isOpponentSide(source, color)) {
            score += Math.max(0, 5 - distance) * 75;
        }
        if (boardControlsPalace(board, source, color.opponent())) {
            score += switch (type) {
                case ROOK, CANNON, KNIGHT -> 260;
                case PAWN -> 360;
                case GUARD, BISHOP -> 240;
                case KING -> 420;
            };
        }
        return score;
    }

    private int cannonLinePressure(Board board, Position cannon, Position enemyKing, int hiddenPhase) {
        int score = 0;
        if (cannon.x() == enemyKing.x()) {
            int between = board.countBetween(cannon, enemyKing);
            if (between == 1) {
                score += CANNON_LINE_PRESSURE_BONUS + hiddenPhase * 3;
            } else if (between == 0) {
                score += CANNON_LINE_PRESSURE_BONUS / 3;
            } else if (between == 2 && hiddenPhase >= 40) {
                score += CANNON_LINE_PRESSURE_BONUS / 2;
            }
        }
        int enemyBackRank = enemyKing.y();
        if (Math.abs(cannon.y() - enemyBackRank) <= 1 && Math.abs(cannon.x() - enemyKing.x()) <= 2) {
            score += CANNON_LINE_PRESSURE_BONUS / 2 + hiddenPhase;
        }
        return score;
    }

    private int rookRibPressure(Board board, Position rook, Position enemyKing, int hiddenPhase) {
        int fileDistance = Math.abs(rook.x() - enemyKing.x());
        if (fileDistance > 1) {
            return 0;
        }
        int score = ROOK_RIB_PRESSURE_BONUS + Math.max(0, 5 - fileDistance * 3) * 20;
        if (rook.x() == enemyKing.x() && board.countBetween(rook, enemyKing) <= 1) {
            score += ROOK_RIB_PRESSURE_BONUS;
        }
        return score + hiddenPhase;
    }

    private int revealOpportunityScore(Board board, PlayerColor color) {
        int hiddenPhase = hiddenPhase(board);
        if (hiddenPhase < 20) {
            return 0;
        }
        int score = 0;
        for (Move move : moveGenerator.generateActions(board, color, 0)) {
            Piece mover = board.get(move.source());
            if (mover == null || mover.visible()) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            int moveScore = REVEAL_BONUS / 10 + expectedHiddenValue(board, color) / 12;
            if (board.get(move.destination()) != null) {
                moveScore += captureValue(board, board.get(move.destination()), move.destination()) / 8;
            }
            if (isForwardReveal(move, color)) {
                moveScore += hiddenPhase / 2;
            }
            score += Math.min(180, moveScore);
        }
        return Math.min(900, score * hiddenPhase / 100);
    }

    private boolean isForwardReveal(Move move, PlayerColor color) {
        int dy = move.destination().y() - move.source().y();
        return Integer.signum(dy) == color.forwardDirection();
    }

    private int xiangqiKnowledgeWeight(Board board) {
        int phase = visiblePhase(board);
        if (phase <= XIANGQI_KNOWLEDGE_START_PHASE) {
            return Math.max(15, phase * 4 / 3);
        }
        if (phase >= XIANGQI_KNOWLEDGE_FULL_PHASE) {
            return 100;
        }
        return 25 + (phase - XIANGQI_KNOWLEDGE_START_PHASE) * 75
                / (XIANGQI_KNOWLEDGE_FULL_PHASE - XIANGQI_KNOWLEDGE_START_PHASE);
    }

    private Move immediateWinningMove(Board board, List<Move> actions, PlayerColor color) {
        for (Move move : actions) {
            Piece captured = board.get(move.destination());
            if (captured != null && knownType(captured) == PieceType.KING) {
                return move;
            }
        }
        for (Move move : actions) {
            Board next = applyForSearch(board, move);
            if (kingMissing(next, color.opponent())) {
                return move;
            }
            if (ruleEngine.isInCheck(next, color.opponent())
                    && !moveGenerator.hasCheckEscape(next, color.opponent())) {
                return move;
            }
        }
        return null;
    }

    private Move forcedMateMove(Board board, List<Move> actions, PlayerColor color) {
        if (strategicPhase(board, color) < LATE_MAJOR_PHASE - 10
                && opponentPlanThreatScore(board, color.opponent()) < 2_800) {
            return null;
        }
        Move best = null;
        int bestScore = 0;
        int searched = 0;
        for (Move move : actions) {
            if (searched++ >= mateSearchBranching()) {
                break;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            if (isImmediateMate(next, color)) {
                return move;
            }
            if (!ruleEngine.isInCheck(next, color.opponent())) {
                continue;
            }
            if (canForceMate(next, color.opponent(), color, mateSearchDepth() - 1)) {
                int score = endgameMateNetScore(next, color)
                        + coordinatedKingAttackScore(next, color)
                        + opponentPlanThreatScore(next, color.opponent());
                Piece captured = board.get(move.destination());
                if (captured != null) {
                    score += captureValue(board, captured, move.destination());
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = move;
                }
            }
        }
        return bestScore >= 2_400 ? best : null;
    }

    private Move urgentForcedMateDefenseMove(Board board, List<Move> actions, PlayerColor color) {
        if (!ruleEngine.isInCheck(board, color)
                && kingDangerScore(board, color) < 5_200
                && opponentPlanThreatScore(board, color) < 4_200) {
            return null;
        }
        if (!canForceMate(board, color.opponent(), color.opponent(), mateSearchDepth() - 1)) {
            return null;
        }
        Move best = null;
        int bestScore = 0;
        for (Move move : actions) {
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            if (canForceMate(next, color.opponent(), color.opponent(), mateSearchDepth() - 2)) {
                continue;
            }
            int score = urgentDefenseScore(board, next, move, color)
                    + Math.max(0, kingDangerScore(board, color) - kingDangerScore(next, color)) * 3
                    + Math.max(0, opponentPlanThreatScore(board, color) - opponentPlanThreatScore(next, color)) * 2;
            Piece captured = board.get(move.destination());
            if (captured != null && captured.color() == color.opponent()) {
                score += captureUrgencyBonus(board, captured, move.destination(), color)
                        + captureValue(board, captured, move.destination()) * 2;
            }
            score -= urgentTradePenalty(board, next, move, color, false);
            Piece mover = board.get(move.source());
            if (mover != null && knownType(mover) == PieceType.KING) {
                score -= 900;
            }
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return bestScore >= 1_200 ? best : null;
    }

    private boolean canForceMate(Board board, PlayerColor sideToMove, PlayerColor attacker, int depth) {
        if (isImmediateMate(board, attacker)) {
            return true;
        }
        if (depth <= 0 || kingMissing(board, attacker) || kingMissing(board, attacker.opponent())) {
            return false;
        }
        List<Move> actions = legalOrderedLimitedMoves(board, sideToMove, sideToMove == attacker);
        if (actions.isEmpty()) {
            return ruleEngine.isInCheck(board, sideToMove) && sideToMove == attacker.opponent();
        }
        if (sideToMove == attacker) {
            int searched = 0;
            for (Move move : actions) {
                if (searched++ >= mateSearchBranching()) {
                    break;
                }
                Board next = applyForSearch(board, move);
                if (!ruleEngine.isInCheck(next, attacker.opponent())) {
                    continue;
                }
                if (canForceMate(next, sideToMove.opponent(), attacker, depth - 1)) {
                    return true;
                }
            }
            return false;
        }
        int searched = 0;
        for (Move move : actions) {
            if (searched++ >= mateSearchBranching()) {
                break;
            }
            Board next = applyForSearch(board, move);
            if (!canForceMate(next, sideToMove.opponent(), attacker, depth - 1)) {
                return false;
            }
        }
        return true;
    }

    private List<Move> legalOrderedLimitedMoves(Board board, PlayerColor color, boolean checksOnly) {
        return orderedMoves(board, color, System.currentTimeMillis(), 0).stream()
                .filter(move -> ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color))
                .filter(move -> !checksOnly || ruleEngine.isInCheck(applyForSearch(board, move), color.opponent()))
                .limit(mateSearchBranching())
                .toList();
    }

    private int mateSearchDepth() {
        return Math.max(1, Math.min(6, config.intValue("search.mateDepth", MATE_SEARCH_DEPTH)));
    }

    private int mateSearchBranching() {
        return Math.max(6, Math.min(30, config.intValue("search.mateBranching", MATE_SEARCH_BRANCHING)));
    }

    private boolean isImmediateMate(Board board, PlayerColor attacker) {
        return kingMissing(board, attacker.opponent())
                || (ruleEngine.isInCheck(board, attacker.opponent())
                && !moveGenerator.hasCheckEscape(board, attacker.opponent()));
    }

    private Move urgentKingDefenseMove(Board board, List<Move> actions, PlayerColor color) {
        int currentDanger = kingDangerScore(board, color);
        if (currentDanger < 2_400 && !ruleEngine.isInCheck(board, color)) {
            return null;
        }
        boolean inCheck = ruleEngine.isInCheck(board, color);
        int bestDefenseLoss = inCheck ? bestCheckDefenseLoss(board, actions, color) : 0;
        Move best = null;
        int bestScore = 0;
        for (Move move : actions) {
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Piece mover = board.get(move.source());
            if (mover == null) {
                continue;
            }
            Board next = applyForSearch(board, move);
            int nextDanger = kingDangerScore(next, color);
            int dangerDrop = currentDanger - nextDanger;
            if (dangerDrop <= 0 && !ruleEngine.isInCheck(board, color)) {
                continue;
            }
            int netDefenseLoss = inCheck ? netCheckDefenseLoss(board, next, move, color) : 0;
            if (inCheck && bestDefenseLoss < Integer.MAX_VALUE
                    && netDefenseLoss >= bestDefenseLoss + value(PieceType.KNIGHT)) {
                continue;
            }
            int score = dangerDrop * 3 + urgentDefenseScore(board, next, move, color);
            Piece captured = board.get(move.destination());
            if (captured != null && captured.color() == color.opponent()) {
                score += captureValue(board, captured, move.destination()) * 3
                        + captureUrgencyBonus(board, captured, move.destination(), color);
            }
            PieceType moverType = knownType(mover);
            if (moverType == PieceType.ROOK || moverType == PieceType.CANNON || moverType == PieceType.KNIGHT) {
                score += DEFENSIVE_MAJOR_RELIEF_BONUS;
            } else if (moverType == PieceType.PAWN) {
                score += 520;
            } else if (moverType == PieceType.KING) {
                score -= ruleEngine.isInCheck(board, color) ? 450 : 2_200;
            }
            score -= costlyCheckBlockPenalty(board, next, move, color);
            score -= netDefenseLoss * 4;
            score -= exposedMovePenalty(board, next, move, color);
            score -= obviousGiftPenalty(board, next, move, color) / 2;
            score -= urgentTradePenalty(board, next, move, color,
                    ruleEngine.isInCheck(board, color));
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        int threshold = ruleEngine.isInCheck(board, color) ? 900 : 1_800;
        return bestScore >= threshold ? best : null;
    }

    private int bestCheckDefenseLoss(Board board, List<Move> actions, PlayerColor color) {
        int best = Integer.MAX_VALUE;
        for (Move move : actions) {
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            best = Math.min(best, netCheckDefenseLoss(board, next, move, color));
        }
        return best;
    }

    private int netCheckDefenseLoss(Board before, Board after, Move move, PlayerColor color) {
        Piece moved = after.get(move.destination());
        if (moved == null || knownType(moved) == PieceType.KING || ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int directLoss = directReplyCaptureLoss(after, move, color, movedValue);
        if (directLoss <= 0) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        int immediateGain = captured == null || captured.color() != color.opponent()
                ? 0
                : captureValue(before, captured, move.destination());
        int compensation = immediateGain
                + Math.max(0, immediateThreatScore(after, color) - immediateThreatScore(before, color)) / 6;
        return Math.max(0, directLoss - compensation);
    }

    private int costlyCheckBlockPenalty(Board before, Board after, Move move, PlayerColor color) {
        if (!ruleEngine.isInCheck(before, color) || ruleEngine.isInCheck(after, color)) {
            return 0;
        }
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || knownType(moved) == PieceType.KING) {
            return 0;
        }
        Piece captured = before.get(move.destination());
        if (captured != null && captured.color() == color.opponent()) {
            return 0;
        }
        int movedValue = pieceSearchValue(after, moved, move.destination());
        int directLoss = directReplyCaptureLoss(after, move, color, movedValue);
        if (directLoss <= 0) {
            return 0;
        }
        int penalty = directLoss * 5;
        PieceType type = knownType(moved);
        if (type == PieceType.ROOK || type == PieceType.CANNON || type == PieceType.KNIGHT) {
            penalty += COSTLY_CHECK_BLOCK_PENALTY + movedValue * 3;
        } else {
            penalty += Math.max(0, movedValue - value(PieceType.PAWN));
        }
        if (defendersValue(after, move.destination(), color) > 0) {
            penalty /= 2;
        }
        return Math.min(30_000, penalty);
    }

    private int kingDangerScore(Board board, PlayerColor color) {
        Position king = board.findKing(color);
        if (king == null) {
            return WIN_SCORE / 2;
        }
        int score = ruleEngine.isInCheck(board, color) ? 8_000 : 0;
        int legalKingMoves = legalKingMoveCount(board, color);
        if (legalKingMoves <= 1) {
            score += 2_600;
        } else if (legalKingMoves == 2) {
            score += 900;
        }
        score += immediateThreatScore(board, color.opponent()) / 2;
        score += coordinatedKingAttackScore(board, color.opponent());
        score += rookCannonBatteryThreatScore(board, color);
        score += opponentPlanThreatScore(board, color);
        score += invadingPieceScore(board, color);
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color.opponent()) {
                continue;
            }
            int distance = manhattan(source, king);
            PieceType type = knownType(piece);
            if (distance <= 4) {
                score += Math.max(0, 5 - distance) * switch (type) {
                    case ROOK -> 520;
                    case CANNON -> 440;
                    case KNIGHT -> 360;
                    case PAWN -> 260;
                    case GUARD, BISHOP -> 120;
                    case KING -> 0;
                };
            }
            if ((type == PieceType.ROOK || type == PieceType.CANNON) && sameFileOrRank(source, king)) {
                score += board.countBetween(source, king) <= (type == PieceType.CANNON ? 2 : 1) ? 900 : 240;
            }
        }
        return Math.min(30_000, score);
    }

    private int rookCannonBatteryThreatScore(Board board, PlayerColor defender) {
        Position king = board.findKing(defender);
        if (king == null) {
            return WIN_SCORE / 2;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != defender.opponent()) {
                continue;
            }
            PieceType type = knownType(piece);
            if (type != PieceType.ROOK && type != PieceType.CANNON) {
                continue;
            }
            if (!sameFileOrRank(source, king)) {
                continue;
            }
            int between = board.countBetween(source, king);
            boolean liveLine = type == PieceType.ROOK ? between <= 1 : between <= 2;
            if (!liveLine) {
                continue;
            }
            int partners = 0;
            for (Position partnerPosition : board.occupiedPositions()) {
                if (partnerPosition.equals(source)) {
                    continue;
                }
                Piece partner = board.get(partnerPosition);
                if (partner == null || partner.color() != defender.opponent()) {
                    continue;
                }
                PieceType partnerType = knownType(partner);
                if ((partnerType == PieceType.ROOK || partnerType == PieceType.CANNON)
                        && sameFileOrRank(partnerPosition, king)) {
                    partners++;
                }
            }
            score += type == PieceType.CANNON ? 720 : 560;
            if (partners > 0) {
                score += 1_450 + partners * 380;
            }
            if (between == 1 && type == PieceType.CANNON) {
                score += 900;
            }
            if (distanceToPalace(source, defender) <= 1) {
                score += 620;
            }
        }
        return Math.min(5_200, score);
    }

    private Move urgentHomeDefenseMove(Board board, List<Move> actions, PlayerColor color) {
        Move best = null;
        int bestScore = 0;
        for (Move move : actions) {
            Piece captured = board.get(move.destination());
            if (captured == null || captured.color() != color.opponent()
                    || !isHomeSide(move.destination(), color)) {
                continue;
            }
            int invaderDanger = homeInvaderDanger(board, move.destination(), color);
            if (captureValue(board, captured, move.destination()) < HIGH_VALUE_PIECE
                    && invaderDanger < HOME_INVADER_PENALTY) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            Piece mover = board.get(move.source());
            int capturedValue = captureValue(board, captured, move.destination());
            int moverValue = mover == null ? 0 : pieceSearchValue(board, mover, move.source());
            int exposure = exposedMovePenalty(board, next, move, color);
            if (capturedValue < HIGH_VALUE_PIECE
                    && moverValue >= HIGH_VALUE_PIECE
                    && exposure > capturedValue
                    && invaderDanger < 2_600) {
                continue;
            }
            int favorableTrade = Math.max(0, capturedValue - moverValue);
            int tradePenalty = urgentTradePenalty(board, next, move, color, invaderDanger >= 3_200);
            int unsoundMajorTrade = unsoundMajorTradeCheckPenalty(board, next, move, color);
            int darkRecaptureRisk = darkUncertainRecaptureRiskPenalty(board, next, move, color);
            if (tradePenalty >= BAD_TRADE_HIERARCHY_PENALTY
                    && favorableTrade == 0
                    && invaderDanger < 3_200
                    && urgentDefenseScore(board, next, move, color) < 1_600) {
                continue;
            }
            if (unsoundMajorTrade >= UNSOUND_MAJOR_TRADE_CHECK_PENALTY
                    && !(ruleEngine.isInCheck(next, color.opponent())
                    && !moveGenerator.hasCheckEscape(next, color.opponent()))) {
                continue;
            }
            if (darkRecaptureRisk >= DARK_RECAPTURE_RISK_PENALTY
                    && !(ruleEngine.isInCheck(next, color.opponent())
                    && !moveGenerator.hasCheckEscape(next, color.opponent()))) {
                continue;
            }
            int score = capturedValue * 5
                    + favorableTrade * 5
                    + invaderDanger * 3
                    + captureUrgencyBonus(board, captured, move.destination(), color)
                    + urgentDefenseScore(board, next, move, color)
                    - moverValue
                    - exposure
                    - tradePenalty
                    - unsoundMajorTrade
                    - darkRecaptureRisk
                    - immediateThreatScore(next, color.opponent()) / 4;
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return bestScore >= 2_400 ? best : null;
    }

    private Move urgentMaterialCaptureMove(Board board, List<Move> actions, PlayerColor color) {
        Move best = null;
        int bestScore = 0;
        for (Move move : actions) {
            Piece captured = board.get(move.destination());
            if (captured == null || captured.color() != color.opponent()
                    || knownType(captured) == PieceType.KING) {
                continue;
            }
            int capturedValue = captureValue(board, captured, move.destination());
            PieceType capturedKnownType = knownType(captured);
            int invaderDanger = homeInvaderDanger(board, move.destination(), color);
            if (capturedKnownType == PieceType.PAWN
                    && capturedValue < HIGH_VALUE_PIECE
                    && invaderDanger < HOME_INVADER_PENALTY) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            int exposure = exposedMovePenalty(board, next, move, color);
            int opponentThreat = immediateThreatScore(next, color.opponent());
            if (opponentThreat >= WIN_SCORE / 4) {
                continue;
            }
            Piece mover = board.get(move.source());
            int revealBonus = mover != null && !mover.visible() ? REVEAL_BONUS : 0;
            int hiddenCaptureBonus = mover != null && !mover.visible()
                    && capturedKnownType != PieceType.PAWN ? HIDDEN_CAPTURE_BONUS : 0;
            int moverValue = mover == null ? 0 : pieceSearchValue(board, mover, move.source());
            if (capturedValue < HIGH_VALUE_PIECE
                    && moverValue >= HIGH_VALUE_PIECE
                    && exposure > capturedValue
                    && invaderDanger < 2_600) {
                continue;
            }
            int favorableTrade = Math.max(0, capturedValue - moverValue);
            int tradePenalty = urgentTradePenalty(board, next, move, color, invaderDanger >= 3_200);
            int unsoundMajorTrade = unsoundMajorTradeCheckPenalty(board, next, move, color);
            int darkRecaptureRisk = darkUncertainRecaptureRiskPenalty(board, next, move, color);
            if (tradePenalty >= BAD_TRADE_HIERARCHY_PENALTY
                    && favorableTrade == 0
                    && invaderDanger < 3_200
                    && urgentDefenseScore(board, next, move, color) < 1_600) {
                continue;
            }
            if (unsoundMajorTrade >= UNSOUND_MAJOR_TRADE_CHECK_PENALTY
                    && !(ruleEngine.isInCheck(next, color.opponent())
                    && !moveGenerator.hasCheckEscape(next, color.opponent()))) {
                continue;
            }
            if (darkRecaptureRisk >= DARK_RECAPTURE_RISK_PENALTY
                    && !(ruleEngine.isInCheck(next, color.opponent())
                    && !moveGenerator.hasCheckEscape(next, color.opponent()))) {
                continue;
            }
            int score = capturedValue * 5
                    + favorableTrade * 5
                    + invaderDanger * 2
                    + revealBonus
                    + hiddenCaptureBonus
                    + captureUrgencyBonus(board, captured, move.destination(), color)
                    + urgentDefenseScore(board, next, move, color)
                    + rootedExchangeScore(board, next, move, color)
                    - moverValue
                    - exposure
                    - tradePenalty
                    - unsoundMajorTrade
                    - darkRecaptureRisk
                    - opponentThreat / 5;
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return bestScore >= 900 ? best : null;
    }

    private Move urgentThreatenedPieceMove(Board board, List<Move> actions, PlayerColor color) {
        int currentExposure = importantPieceExposure(board, color);
        if (currentExposure < 1_000) {
            return null;
        }
        Move best = null;
        int bestScore = 0;
        for (Move move : actions) {
            Piece mover = board.get(move.source());
            if (mover == null || knownType(mover) == PieceType.KING || knownType(mover) == PieceType.PAWN) {
                continue;
            }
            if (attackersValue(board, move.source(), color.opponent()) == 0) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            int exposureDrop = currentExposure - importantPieceExposure(next, color);
            if (exposureDrop <= 0) {
                continue;
            }
            int score = exposureDrop
                    + tacticalMoveScore(board, move, color)
                    + rootedExchangeScore(board, next, move, color)
                    - exposedMovePenalty(board, next, move, color)
                    - urgentTradePenalty(board, next, move, color, exposureDrop >= 2_400)
                    - immediateThreatScore(next, color.opponent()) / 4;
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return bestScore >= 900 ? best : null;
    }

    private Move usefulRevealMove(Board board, List<Move> actions, PlayerColor color) {
        int ownHidden = hiddenPieceCount(board, color);
        int hiddenPhase = hiddenPhase(board);
        int visiblePhase = visiblePhase(board);
        int strategicPhase = strategicPhase(board, color);
        int coreHidden = remainingCoreHiddenCount(board, color);
        boolean enoughVisibleFirepower = visibleTypeCount(board, color, PieceType.ROOK) >= 2
                && visibleTypeCount(board, color, PieceType.CANNON) >= 1;
        if (ownHidden == 0 || (ownHidden < 3 && coreHidden == 0 && strategicPhase < LATE_MAJOR_PHASE)) {
            return null;
        }
        boolean needsRecoveryReveal = materialBalance(board, color) < -value(PieceType.CANNON)
                || activeMajorCount(board, color) == 0
                || visibleMajorCount(board, color) < visibleMajorCount(board, color.opponent());
        if (enoughVisibleFirepower) {
            needsRecoveryReveal = needsRecoveryReveal && opponentPlanThreatScore(board, color) >= 3_200;
        }
        int revealSuppressionExposure = revealSuppressionExposure(board, color);
        if (revealSuppressionExposure >= (needsRecoveryReveal ? 2_800 : 1_300)) {
            return null;
        }
        Move best = null;
        int bestScore = 0;
        for (Move move : actions) {
            Piece mover = board.get(move.source());
            if (mover == null || mover.visible()) {
                continue;
            }
            if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color)) {
                continue;
            }
            Board next = applyForSearch(board, move);
            int opponentThreat = immediateThreatScore(next, color.opponent());
            if (opponentThreat >= WIN_SCORE / 4) {
                continue;
            }
            int score = revealMoveScore(board, move, color, next)
                    + revealDevelopmentBonus(board, move, color, ownHidden, hiddenPhase)
                    + endgameRevealBonus(board, move, color, ownHidden, strategicPhase, next)
                    + openingRevealStrategyBonus(board, next, move, color)
                    + continuedRevealBonus(board, next, move, color, ownHidden, hiddenPhase)
                    + palaceHiddenActivationScore(board, next, move, color)
                    + recoveryRevealProgressScore(board, next, move, color)
                    + urgentDefenseScore(board, next, move, color)
                    - exposedMovePenalty(board, next, move, color)
                    - urgentTradePenalty(board, next, move, color, false)
                    - opponentThreat / 5;
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        int threshold = needsRecoveryReveal ? 900
                : strategicPhase >= LATE_MAJOR_PHASE && ownHidden <= 2 ? 1_050
                : ownHidden >= 5 ? 1_450 : 1_850;
        if (enoughVisibleFirepower && !needsRecoveryReveal) {
            threshold += 1_150;
            if (strategicPhase >= LATE_MAJOR_PHASE) {
                threshold += 450;
            }
        }
        return bestScore >= threshold ? best : null;
    }

    private int openingRevealStrategyBonus(Board before, Board after, Move move, PlayerColor color) {
        if (visiblePhase(before) > 45) {
            return 0;
        }
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || mover.visible()) {
            return 0;
        }
        int score = 0;
        PieceType moveType = knownType(mover);
        int highRemaining = remainingHighValueHiddenCount(before, color);
        if (highRemaining >= 3 && (moveType == PieceType.ROOK || moveType == PieceType.CANNON || moveType == PieceType.KNIGHT)) {
            score += 520;
        }
        if (isForwardReveal(move, color)) {
            score += 260;
        }
        if (isOpponentSide(move.destination(), color)) {
            score += 220;
        }
        score += Math.max(0, layoutPatternScore(after, color) - layoutPatternScore(before, color)) / 2;
        score += Math.max(0, pieceRoleScore(after, color) - pieceRoleScore(before, color)) / 2;
        if (attackersValue(after, move.destination(), color.opponent()) > 0
                && defendersValue(after, move.destination(), color) == 0) {
            score -= likelyHighHiddenValue(before, color, move.destination());
        }
        if (hiddenInvaderInformationDanger(after, move.destination(), color.opponent()) > 800) {
            score += 240;
        }
        return Math.max(-1_200, Math.min(1_400, score));
    }

    private int continuedRevealBonus(
            Board before,
            Board after,
            Move move,
            PlayerColor color,
            int ownHidden,
            int hiddenPhase) {
        Piece mover = before.get(move.source());
        Piece moved = after.get(move.destination());
        if (mover == null || moved == null || mover.visible() || ownHidden < 3 || hiddenPhase < 22) {
            return 0;
        }
        int coreHidden = remainingCoreHiddenCount(before, color);
        if (coreHidden <= 0) {
            return 0;
        }
        int attackers = attackersValue(after, move.destination(), color.opponent());
        int defenders = defendersValue(after, move.destination(), color);
        if (attackers > 0 && defenders == 0) {
            return 0;
        }
        int score = ownHidden * 95 + coreHidden * 360 + hiddenPhase * 5;
        if (isForwardReveal(move, color)) {
            score += 360;
        }
        if (isOpponentSide(move.destination(), color)) {
            score += 220;
        }
        if (attackers == 0) {
            score += 260;
        } else {
            score += Math.min(defenders, pieceSearchValue(after, moved, move.destination())) / 5;
        }
        int exposureDelta = importantPieceExposure(after, color) - importantPieceExposure(before, color);
        if (exposureDelta > 0) {
            score -= exposureDelta / 2;
        }
        score += Math.max(0, hiddenInformationScore(after, color) - hiddenInformationScore(before, color)) / 2;
        return Math.max(0, Math.min(CONTINUED_REVEAL_BONUS, score));
    }

    private int endgameRevealBonus(
            Board before,
            Move move,
            PlayerColor color,
            int ownHidden,
            int visiblePhase,
            Board after) {
        if (visiblePhase < LATE_MAJOR_PHASE || ownHidden > 2) {
            return 0;
        }
        int attackers = attackersValue(after, move.destination(), color.opponent());
        int defenders = defendersValue(after, move.destination(), color);
        if (attackers > 0 && defenders == 0) {
            return 0;
        }
        int bonus = ENDGAME_REVEAL_BONUS + (LATE_MAJOR_PHASE + ownHidden * 120);
        if (isOpponentSide(move.destination(), color)) {
            bonus += 260;
        }
        if (before.get(move.destination()) != null) {
            bonus += 420;
        }
        return bonus;
    }

    private int revealDevelopmentBonus(Board board, Move move, PlayerColor color, int ownHidden, int hiddenPhase) {
        int score = ownHidden * 120 + hiddenPhase * 4;
        if (isForwardReveal(move, color)) {
            score += 360;
        }
        if (board.get(move.destination()) == null) {
            score += 120;
        }
        int destinationRank = color == PlayerColor.RED
                ? move.destination().y()
                : Position.HEIGHT - 1 - move.destination().y();
        if (destinationRank >= 3 && destinationRank <= 6) {
            score += 220;
        }
        return score;
    }

    private int hiddenPieceCount(Board board, PlayerColor color) {
        int count = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece != null && piece.color() == color && !piece.visible()) {
                count++;
            }
        }
        return count;
    }

    private int attackersValue(Board board, Position target, PlayerColor attackerColor) {
        int lowest = Integer.MAX_VALUE;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece != null && piece.color() == attackerColor
                    && ruleEngine.canMove(board, source, target, attackerColor)) {
                lowest = Math.min(lowest, pieceSearchValue(board, piece, source));
            }
        }
        return lowest == Integer.MAX_VALUE ? 0 : lowest;
    }

    private int legalAttackersValue(Board board, Position target, PlayerColor attackerColor) {
        int lowest = Integer.MAX_VALUE;
        for (Position source : board.occupiedPositions()) {
            if (source.equals(target)) {
                continue;
            }
            Piece piece = board.get(source);
            if (piece != null && piece.color() == attackerColor
                    && ruleEngine.canMoveAndKeepKingSafe(board, source, target, attackerColor)) {
                lowest = Math.min(lowest, pieceSearchValue(board, piece, source));
            }
        }
        return lowest == Integer.MAX_VALUE ? 0 : lowest;
    }

    private int exchangeSequenceGain(Board board, Position target, PlayerColor attackerColor) {
        return exchangeSequenceGain(board, target, attackerColor, 0);
    }

    private int exchangeSequenceGain(Board board, Position target, PlayerColor attackerColor, int depth) {
        if (depth >= EXCHANGE_SEQUENCE_DEPTH) {
            return 0;
        }
        Piece victim = board.get(target);
        if (victim == null || victim.color() == attackerColor || knownType(victim) == PieceType.KING) {
            return 0;
        }
        int best = 0;
        for (Move capture : moveGenerator.generateActions(board, attackerColor, 0)) {
            if (!capture.destination().equals(target)
                    || !ruleEngine.canMoveAndKeepKingSafe(board, capture.source(), capture.destination(), attackerColor)) {
                continue;
            }
            Piece attacker = board.get(capture.source());
            if (attacker == null) {
                continue;
            }
            int capturedValue = captureValue(board, victim, target);
            Board afterCapture = applyForSearch(board, capture);
            int replyGain = exchangeSequenceGain(afterCapture, target, attackerColor.opponent(), depth + 1);
            int net = capturedValue - replyGain;
            if (ruleEngine.isInCheck(afterCapture, attackerColor.opponent())
                    && !moveGenerator.hasCheckEscape(afterCapture, attackerColor.opponent())) {
                net += WIN_SCORE / 4;
            }
            best = Math.max(best, net);
        }
        return Math.max(0, best);
    }

    private int defendersValue(Board board, Position target, PlayerColor defenderColor) {
        Piece protectedPiece = board.get(target);
        if (protectedPiece == null || protectedPiece.color() != defenderColor) {
            return 0;
        }
        Board targetRemoved = board.copy();
        targetRemoved.remove(target);
        int lowest = Integer.MAX_VALUE;
        for (Position source : board.occupiedPositions()) {
            if (source.equals(target)) {
                continue;
            }
            Piece piece = board.get(source);
            if (piece != null && piece.color() == defenderColor
                    && ruleEngine.canMoveAndKeepKingSafe(targetRemoved, source, target, defenderColor)) {
                lowest = Math.min(lowest, pieceSearchValue(board, piece, source));
            }
        }
        return lowest == Integer.MAX_VALUE ? 0 : lowest;
    }

    private int hangingPieceScore(Board board, PlayerColor aiColor) {
        int score = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || knownType(piece) == PieceType.KING) {
                continue;
            }
            int attackers = attackersValue(board, position, piece.color().opponent());
            if (attackers == 0) {
                continue;
            }
            int defenders = defendersValue(board, position, piece.color());
            int value = pieceSearchValue(board, piece, position);
            int risk = defenders == 0 ? value * 3 / 2 : Math.max(60, value / 3);
            if (value >= HIGH_VALUE_PIECE) {
                risk += defenders == 0 ? EXPOSED_MAJOR_PIECE_PENALTY : EXPOSED_MAJOR_PIECE_PENALTY / 2;
            }
            score += piece.color() == aiColor ? -risk : risk;
        }
        return score;
    }

    private int exposedImportantPieces(Board board, PlayerColor color) {
        return Math.min(10_000, importantPieceExposure(board, color) / 2);
    }

    private int revealSuppressionExposure(Board board, PlayerColor color) {
        int penalty = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                continue;
            }
            PieceType type = knownType(piece);
            int value = pieceSearchValue(board, piece, position);
            if (value < HIGH_VALUE_PIECE) {
                continue;
            }
            int attackers = attackersValue(board, position, color.opponent());
            if (attackers == 0) {
                continue;
            }
            int defenders = defendersValue(board, position, color);
            boolean coreMajor = type == PieceType.ROOK || type == PieceType.CANNON;
            boolean criticalKnight = type == PieceType.KNIGHT && defenders == 0;
            if (!coreMajor && !criticalKnight) {
                continue;
            }
            int base = value * 2 + EXPOSED_MAJOR_PIECE_PENALTY;
            penalty += defenders == 0 ? base : base / 2;
        }
        return Math.min(20_000, penalty);
    }

    private int importantPieceExposure(Board board, PlayerColor color) {
        int penalty = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color || knownType(piece) == PieceType.KING) {
                continue;
            }
            int value = pieceSearchValue(board, piece, position);
            if (value < HIGH_VALUE_PIECE) {
                continue;
            }
            int attackers = attackersValue(board, position, color.opponent());
            if (attackers == 0) {
                continue;
            }
            int defenders = defendersValue(board, position, color);
            int base = value * 2 + EXPOSED_MAJOR_PIECE_PENALTY;
            penalty += defenders == 0 ? base : base * 2 / 3;
        }
        return Math.min(20_000, penalty);
    }

    private int mobilityDelta(Board before, Board after, PlayerColor color) {
        int ownBefore = moveGenerator.generateActions(before, color, 0).size();
        int ownAfter = moveGenerator.generateActions(after, color, 0).size();
        int oppBefore = moveGenerator.generateActions(before, color.opponent(), 0).size();
        int oppAfter = moveGenerator.generateActions(after, color.opponent(), 0).size();
        return (ownAfter - ownBefore) - (oppAfter - oppBefore);
    }

    private int kingPressure(Board board, PlayerColor attackingColor) {
        Position king = board.findKing(attackingColor.opponent());
        if (king == null) {
            return WIN_SCORE / 4;
        }
        int pressure = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != attackingColor) {
                continue;
            }
            int distance = Math.abs(source.x() - king.x()) + Math.abs(source.y() - king.y());
            pressure += Math.max(0, 10 - distance) * switch (knownType(piece)) {
                case ROOK -> 9;
                case CANNON -> 7;
                case KNIGHT -> 6;
                case PAWN -> 3;
                case GUARD, BISHOP -> 1;
                case KING -> 0;
            };
        }
        return pressure;
    }

    private int attackScore(Board board, Position source, PlayerColor color) {
        int score = 0;
        for (Position targetPosition : board.occupiedPositions()) {
            Piece target = board.get(targetPosition);
            if (target != null && target.color() != color
                    && ruleEngine.canMove(board, source, targetPosition, color)) {
                score += captureValue(board, target, targetPosition) / ATTACK_BONUS;
            }
        }
        return score;
    }

    private Board applyForSearch(Board board, Move move) {
        return applyForSearch(board, move, null);
    }

    private Board applyForSearch(Board board, Move move, PieceType revealedType) {
        Board copied = board.copy();
        Piece mover = copied.remove(move.source());
        copied.set(move.destination(), mover);
        if (mover != null && !mover.visible()) {
            PieceType expectedType = revealedType == null
                    ? expectedRevealType(board, mover.color(), move.destination())
                    : revealedType;
            copied.set(move.destination(), new Piece(mover.color(), expectedType, mover.hiddenMoveType(), true));
        }
        return copied;
    }

    private int pieceSearchValue(Board board, Piece piece, Position position) {
        if (piece.visible()) {
            return value(piece.type());
        }
        return expectedHiddenValue(board, piece.color(), position);
    }

    private int expectedHiddenValue(Board board, PlayerColor color) {
        return expectedHiddenValue(board, color, null);
    }

    private int expectedHiddenValue(Board board, PlayerColor color, Position position) {
        Map<PieceType, Integer> counts = remainingHiddenTypeCounts(board, color);
        int total = 0;
        int weighted = 0;
        for (Map.Entry<PieceType, Integer> entry : counts.entrySet()) {
            total += entry.getValue();
            weighted += expectedTypeValue(color, entry.getKey(), position) * entry.getValue();
        }
        return total == 0 ? value(PieceType.PAWN) : weighted / total;
    }

    private int expectedTypeValue(PlayerColor color, PieceType type, Position position) {
        int score = value(type);
        if (position != null) {
            score += activityBonus(new Piece(color, type, type, true), position) / 2;
            score += revealPositionBonus(color, type, position);
        }
        return score;
    }

    private int revealPositionBonus(PlayerColor color, PieceType type, Position position) {
        int rank = color == PlayerColor.RED ? position.y() : Position.HEIGHT - 1 - position.y();
        int fileCenter = 4 - Math.abs(position.x() - 4);
        return switch (type) {
            case ROOK -> fileCenter * 18 + rank * 10;
            case CANNON -> fileCenter * 14 + (rank >= 5 ? 90 : 35);
            case KNIGHT -> fileCenter * 12 + (rank >= 3 && rank <= 7 ? 70 : 15);
            case PAWN -> rank * 18 + (rank >= 5 ? fileCenter * 14 : 0);
            case GUARD -> inPalace(position, color) ? 45 : 18;
            case BISHOP -> ownSide(position, color) ? 38 : 24;
            case KING -> 0;
        };
    }

    private int visiblePhase(Board board) {
        int visible = 0;
        int hidden = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || knownType(piece) == PieceType.KING) {
                continue;
            }
            if (piece.visible()) {
                visible++;
            } else {
                hidden++;
            }
        }
        int total = visible + hidden;
        return total == 0 ? 100 : visible * 100 / total;
    }

    private int strategicPhase(Board board, PlayerColor color) {
        int phase = visiblePhase(board);
        phase += Math.min(18, invadingPieceScore(board, color) / 320);
        if (legalKingMoveCount(board, color) <= 1) {
            phase += 12;
        }
        if (hiddenPieceCount(board, color) <= 2) {
            phase += 6;
        }
        if (ruleEngine.isInCheck(board, color)) {
            phase += 10;
        }
        return Math.min(100, phase);
    }

    private int hiddenPhase(Board board) {
        return 100 - visiblePhase(board);
    }

    private int remainingHighValueHiddenCount(Board board, PlayerColor color) {
        Map<PieceType, Integer> counts = remainingHiddenTypeCounts(board, color);
        return counts.getOrDefault(PieceType.ROOK, 0)
                + counts.getOrDefault(PieceType.CANNON, 0)
                + counts.getOrDefault(PieceType.KNIGHT, 0);
    }

    private int remainingCoreHiddenCount(Board board, PlayerColor color) {
        Map<PieceType, Integer> counts = remainingHiddenTypeCounts(board, color);
        return counts.getOrDefault(PieceType.ROOK, 0)
                + counts.getOrDefault(PieceType.CANNON, 0);
    }

    private PieceType expectedRevealType(Board board, PlayerColor color, Position position) {
        int expectedValue = expectedHiddenValue(board, color, position);
        PieceType best = PieceType.PAWN;
        int bestDistance = Integer.MAX_VALUE;
        for (Map.Entry<PieceType, Integer> entry : remainingHiddenTypeCounts(board, color).entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            int distance = Math.abs(expectedTypeValue(color, entry.getKey(), position) - expectedValue);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getKey();
            }
        }
        return best;
    }

    private Map<PieceType, Integer> remainingHiddenTypeCounts(Board board, PlayerColor color) {
        Map<PieceType, Integer> counts = initialHiddenTypeCounts();
        int hiddenPieces = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color) {
                continue;
            }
            if (piece.visible() && piece.type() != PieceType.KING) {
                counts.computeIfPresent(piece.type(), (type, count) -> Math.max(0, count - 1));
            } else if (!piece.visible()) {
                hiddenPieces++;
            }
        }
        if (hiddenPieces == 0) {
            counts.replaceAll((type, count) -> 0);
        }
        return counts;
    }

    private Map<PieceType, Integer> initialHiddenTypeCounts() {
        Map<PieceType, Integer> counts = new EnumMap<>(PieceType.class);
        counts.put(PieceType.ROOK, 2);
        counts.put(PieceType.KNIGHT, 2);
        counts.put(PieceType.CANNON, 2);
        counts.put(PieceType.BISHOP, 2);
        counts.put(PieceType.GUARD, 2);
        counts.put(PieceType.PAWN, 5);
        counts.put(PieceType.KING, 0);
        return counts;
    }

    private PieceType knownType(Piece piece) {
        return piece.visible() ? piece.type() : piece.hiddenMoveType();
    }

    private String heuristicKey(String prefix, Board board, PlayerColor color) {
        return prefix + "|" + color.name() + "|" + knownPositionKey(board);
    }

    public static String knownPositionKey(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = board.get(new Position(x, y));
                if (piece == null) {
                    sb.append(".");
                } else if (piece.visible()) {
                    sb.append(piece.key());
                } else {
                    sb.append(piece.color().name())
                            .append(":HIDDEN:")
                            .append(piece.hiddenMoveType().name())
                            .append(":0");
                }
                sb.append("|");
            }
        }
        return sb.toString();
    }

    private String mirroredKnownPositionKey(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = board.get(new Position(Position.WIDTH - 1 - x, y));
                if (piece == null) {
                    sb.append(".");
                } else if (piece.visible()) {
                    sb.append(piece.key());
                } else {
                    sb.append(piece.color().name())
                            .append(":HIDDEN:")
                            .append(piece.hiddenMoveType().name())
                            .append(":0");
                }
                sb.append("|");
            }
        }
        return sb.toString();
    }

    private Move mirrorMove(Move move) {
        Position source = mirrorPosition(move.source());
        Position destination = mirrorPosition(move.destination());
        return move.flipOnly()
                ? Move.flip(source, move.turnStartTime())
                : Move.move(source, destination, move.turnStartTime());
    }

    private Position mirrorPosition(Position position) {
        return new Position(Position.WIDTH - 1 - position.x(), position.y());
    }

    private boolean kingMissing(Board board, PlayerColor color) {
        return board.findKing(color) == null;
    }

    private boolean inPalace(Position position, PlayerColor color) {
        boolean xOk = position.x() >= 3 && position.x() <= 5;
        boolean yOk = color == PlayerColor.RED
                ? position.y() >= 0 && position.y() <= 2
                : position.y() >= 7 && position.y() <= 9;
        return xOk && yOk;
    }

    private boolean ownSide(Position position, PlayerColor color) {
        return color == PlayerColor.RED ? position.y() <= 4 : position.y() >= 5;
    }

    private boolean isHomeSide(Position position, PlayerColor color) {
        return ownSide(position, color);
    }

    private boolean isOpponentSide(Position position, PlayerColor color) {
        return !ownSide(position, color);
    }

    private int forwardRank(Position position, PlayerColor color) {
        return color == PlayerColor.RED ? position.y() : Position.HEIGHT - 1 - position.y();
    }

    private int activityBonus(Piece piece, Position position) {
        int centerDistance = Math.abs(position.x() - 4) + Math.abs(position.y() - 4);
        int center = Math.max(0, 8 - centerDistance) * 4;
        int forward = piece.color() == PlayerColor.RED ? position.y() : 9 - position.y();
        return switch (knownType(piece)) {
            case ROOK -> center;
            case CANNON -> center + 8;
            case KNIGHT -> center + 10;
            case PAWN -> forward * 12 + center / 2;
            case GUARD, BISHOP -> center / 2;
            case KING -> 0;
        };
    }

    private int value(PieceType type) {
        return switch (type) {
            case KING -> 100_000;
            case ROOK -> 1_120;
            case CANNON -> 660;
            case KNIGHT -> 520;
            case GUARD -> 190;
            case BISHOP -> 190;
            case PAWN -> 180;
        };
    }

    private enum Bound {
        EXACT,
        LOWER,
        UPPER
    }

    private record TranspositionEntry(int depthBudget, int score, Bound bound, String bestMoveNotation) {
    }

    private record SearchResult(Move move, int score, int margin, boolean timedOut) {
        private static SearchResult ok(Move move, int score, int margin) {
            return new SearchResult(move, score, margin, false);
        }

        private static SearchResult timeout(Move move, int score, int margin) {
            return new SearchResult(move, score, margin, true);
        }
    }
}
