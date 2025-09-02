import { registerAs } from "@nestjs/config";

export default registerAs('gateway_external_api', () => ({
    auth_backend_endpoint: "/ext/auth",
    libraries_backend_endpoint: "/ext/libraries",
    releases_backend_endpoint: "/ext/releases",
    components_backend_endpoint: "/ext/core-component",
    components_children_backend_endpoint: "/ext/core-component/children",
    components_schema_backend_endpoint: "/ext/core-component/export/standalone",
    bie_backend_endpoint: "/ext/bie-list",
    bie_generate_backend_endpoint: "/ext/bie/generate",
    bie_packages_backend_endpoint: "/ext/bie-packages",
    bie_packages_bies_backend_endpoint: '/ext/bie-packages/bies',
    component_metadata_page_size: -1,
    bie_metadata_page_size: -1,
}));
