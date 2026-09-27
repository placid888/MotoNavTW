#pragma once
#include <cstdint>
#include <vector>
#include <string>

namespace moto::ble {
    constexpr std::uint8_t kFrameMagic = 0xB7;
    constexpr std::uint8_t kProtocolVersion = 1;
}