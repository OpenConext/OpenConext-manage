import {useNavigate, useParams} from "react-router-dom";


const withRouterHooks = WrappedComponent => props => { // eslint-disable-line react/display-name
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
