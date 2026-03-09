import React, {useEffect, useState} from "react";

import "./SFO.scss";


export default function SFO({
                                configuration,
                                data,
                                isNew,
                                errors,
                                onChange,
                                onError
                            }) {

    useEffect(() => {
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);


    return (
        <section className="metadata-sfo">
            {/*{JSON.stringify(errors)}*/}
            <section className="sfo">
                {JSON.stringify(configuration)}
            </section>
        </section>
    );
}
