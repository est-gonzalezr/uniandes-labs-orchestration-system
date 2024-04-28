
def ganancia(a, b, c, d) -> dict:
    max_dict = a

    max_dict = compare(max_dict, b)
    max_dict = compare(max_dict, c)
    max_dict = compare(max_dict, d)

    return max_dict


def compare(a, b):
    if a["valor"] > b["valor"]:
        return a
    elif a["valor"] == b["valor"]:
        if a["tipo"] < b["tipo"]:
            return a
        else:
            return b
    else:
        return b



a = {"valor": 6, "tipo": "A"}
b = {"valor": 1, "tipo": "B"}
c = {"valor": 2, "tipo": "C"}
d = {"valor": 2, "tipo": "D"}

print(ganancia(a, b, c, d))
