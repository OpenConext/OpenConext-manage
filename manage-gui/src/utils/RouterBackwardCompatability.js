import {useParams} from "react-router-dom";

const {useNavigate} = require("react-router-dom");

const withRouterHooks = WrappedComponent => props => {
    const navigate = useNavigate();
    const params = useParams();

    return (
        <WrappedComponent
            {...props}
            navigate={navigate}
            params={params}
        />
    );
};
export default withRouterHooks;