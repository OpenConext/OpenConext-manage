import React from "react";
import I18n from "i18n-js";
import {stop} from "../utils/Utils";
import "./StepupManagement.scss";
import PolicyPlayground from "../components/PolicyPlaygound";
import withRouterHooks from "../utils/RouterBackwardCompatability";
import PolicyConflicts from "../components/PolicyConflicts";
import {useParams} from "react-router-dom";

export const StepupManagement = ({configuration}) => {
    const {tab = ""} = useParams();
    const [currentTab, setCurrentTab] = useState(tab);
    const navigate = useNavigate();
    const tabs = ["sfo","institutions"]

    const switchTab = tab => e => {
        stop(e);
        setCurrentTab(tab)
        navigate(`/policies/${tab}`);
    };

    const renderTab = (tab, selectedTab) =>
        <span key={tab}
              className={tab === selectedTab ? "active" : ""}
              onClick={switchTab(tab)}>
            {I18n.t(`stepupManagement.${tab}`)}
        </span>;

    const renderCurrentTab = selectedTab => {
        switch (selectedTab) {
            case "sfo" :
                return <SFO configuration={configuration}/>;
            case "institutions" :
                return <Institutions configuration={configuration}/>;
        }
    };

        return (
            <div className="mod-stepup-management">
                <section className="tabs">
                    {tabs.map(tab => renderTab(tab, currentTab))}
                </section>
                {renderCurrentTab(currentTab)}
            </div>
        );

}
