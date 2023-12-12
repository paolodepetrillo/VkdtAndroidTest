cmake_minimum_required(VERSION 3.22.1)

project("vkdtandroidtest")

include(ExternalProject)
include(ProcessorCount)

add_compile_definitions(VK_NO_PROTOTYPES)

add_library(${CMAKE_PROJECT_NAME} SHARED
        vkdtlib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        jnigraphics
        vkdt)

configure_file(vkdt_config.mk.in ${CMAKE_CURRENT_BINARY_DIR}/vkdt_config.mk)

ProcessorCount(NPROC)
if(NOT NPROC EQUAL 0)
    set(VKDT_MAKEFLAGS -j${NPROC})
endif()
SET(VKDT_PREFIX ${CMAKE_CURRENT_BINARY_DIR}/vkdt-prefix)
SET(VKDT_ROOT ${VKDT_PREFIX}/src/vkdt_project)
ExternalProject_Add(vkdt_project
        PREFIX ${VKDT_PREFIX}
        GIT_REPOSITORY /home/paolo/vk/vkdtp
        GIT_TAG android-test
        CONFIGURE_COMMAND cp "${CMAKE_CURRENT_BINARY_DIR}/vkdt_config.mk" "${VKDT_ROOT}/bin/config.mk"
        BUILD_COMMAND ${CMAKE_COMMAND} -E env MAKEFLAGS=${VKDT_MAKEFLAGS}
            make -C "${VKDT_ROOT}/bin" lib
        BUILD_IN_SOURCE true
        INSTALL_COMMAND bash ${CMAKE_SOURCE_DIR}/gen_vkdtbase.sh ${VKDT_ROOT}/vkdtbase.zip ${VKDT_ROOT}
        UPDATE_COMMAND ""
        BUILD_BYPRODUCTS ${VKDT_ROOT}/bin/libvkdt.so)

add_library(vkdt SHARED IMPORTED)
include_directories(${VKDT_ROOT}/src ${VKDT_ROOT}/ext/volk)
set_target_properties(vkdt PROPERTIES IMPORTED_LOCATION ${VKDT_ROOT}/bin/libvkdt.so)

add_dependencies(${CMAKE_PROJECT_NAME} vkdt_project vkdt)

add_custom_command(
        TARGET vkdt_project
        POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
            ${VKDT_ROOT}/vkdtbase.zip
            ${VKDT_ASSETS_DIR}/vkdtbase.zip)
