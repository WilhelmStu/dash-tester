def response(flow):
    print("")
    print("=" * 50)
    print(flow.request.method + " " + flow.request.path + " " + flow.request.http_version)

    if "video/chunk" in flow.request.path:
        print("Response time:", end=" ")
        for k, v in flow.response.headers.items():
            if "DATE" in k.upper():
                print(v)
