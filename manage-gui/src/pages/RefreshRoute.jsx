import {useEffect} from "react";
import "./NotFound.scss";
import {useNavigate, useParams} from "react-router-dom";


export default function RefreshRoute() {

    const {path} = useParams();

    const navigate = useNavigate();

    useEffect(() => {
        const decodedPath = decodeURIComponent(path);
        navigate(decodedPath);
    }, [path, navigate]);

    return null;
}
