/**
 * Enum to hold accepted commands and their string value
 */

public enum Commands {
    getQuickBalance {
        public String toString() {
            return "getQuickBalance";
        }
    },
    getSyncedBalance{
        public String toString() {
            return "getSyncedBalance";
        }
    },
    getSyncedBalanceNaive{
        public String toString() {
            return "getSyncedBalanceNaive";
        }
    },
    deposit{
        public String toString() {
            return "deposit";
        }
    },
    addInterest{
        public String toString() {
            return "addInterest";
        }
    },
    getHistory{
        public String toString() {
            return "getHistory";
        }
    },
    checkTxStatus{
        public String toString() {
            return "checkTxStatus";
        }
    },
    cleanHistory{
        public String toString() {
            return "cleanHistory";
        }
    },
    memberInfo{
        public String toString() {
            return "memberInfo";
        }
    },
    sleep{
        public String toString() {
            return "sleep";
        }
    },
    exit{
        public String toString() {
            return "exit";
        }
    }
}
